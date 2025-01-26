package qrcode;
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11+
//DEPS com.google.zxing:core:3.4.0
//DEPS com.google.zxing:javase:3.4.0
//DEPS info.picocli:picocli:4.7.6
//DEPS info.picocli:picocli-codegen:4.5.0
//DEPS org.apache.xmlgraphics:batik-transcoder:1.17
//DEPS com.twelvemonkeys.imageio:imageio-batik:3.9.4

//JAVA_OPTIONS -Dapple.awt.UIElement=true

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.twelvemonkeys.image.ResampleOp;
import com.twelvemonkeys.imageio.plugins.svg.SVGReadParam;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

class DimensionsConverter implements ITypeConverter<Dimension> {
    public Dimension convert(String value) throws Exception {
        String[] dim = value.split("[x,:]");

        if (dim.length < 1 && dim.length > 2) {
            throw new IllegalArgumentException("Invalid dimensions " + value);
        }

        int width = Integer.parseInt(dim[0]);
        int height = dim.length == 2 ? Integer.parseInt(dim[1]) : width;

        return new Dimension(width, height);
    }
}

class ColorConverter implements ITypeConverter<Color> {
    public Color convert(String value) throws Exception {
        Pattern colorPattern = Pattern.compile("#[A-F0-9]{6}");

        if (!colorPattern.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid color " + value + ". Must be css color in hexadecimal format #RRGGBB");
        }

        return Color.decode(value);
    }
}

@Command(name = "qrcode", mixinStandardHelpOptions = true, version = "qrcode 0.1", description = "Make a QR code with an overlay image. Inspired by https://hollycummins.com/creating-QR-codes/")
class main implements Callable<Integer> {

    @Parameters(index = "0", description = "Text to encode", defaultValue = "http://placekitten.com/g/1024/1024")
    String text;

    @Option(names = { "-i", "--image" }, description = "Image to overlay", required = true, defaultValue = "https://placekitten.com/128/128")
    URI imagePath;

    @Option(names = { "-o", "--output" }, description = "Output file", defaultValue = "qrcode.png")
    Path outPath;

    @Option(names = { "-qrc", "--qr-color" }, description = "The qr code color", defaultValue = "#000000", converter = ColorConverter.class)
    Color qrColor;

    @Option(names = { "-od",
            "--overlay-dimensions" }, description = "Dimension to apply to overlay", converter = DimensionsConverter.class)
    Dimension overlayDimensions;

    public static void main(String[] args) throws Exception {

        new picocli.CommandLine(new main()).execute(args);

    }

    public Integer call() {
        writeQrCode(text, Path.of("").toUri().resolve(imagePath), outPath, 640, qrColor);

        if (outPath.toFile().exists()) {
            System.out.println("Created QR code at " + outPath);
            System.exit(ExitCode.OK); // hard exit to avoid OSX AWT delay
        } else {
            System.out.println("Could not create QR code at " + outPath);
        }
        return ExitCode.OK;
    }

    private void writeQrCode(String text, URI imagePath, Path outPath, int width, Color color) {
        try {
            Map<EncodeHintType, ErrorCorrectionLevel> hints = new HashMap<>();

            // Specify the error correction, to allow the QR code to tolerate errors, such
            // as
            // a great big picture plunked in the middle
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

            // QR codes are square
            int height = width;

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width,
                    height, hints);
            // Load QR image
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix,
                    new MatrixToImageConfig(
                            color.getRGB(),
                            0xFFFFFFFF));
            // Initialize combined image
            BufferedImage combined = new BufferedImage(qrImage.getHeight(), qrImage.getWidth(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) combined.getGraphics();

            // Write QR code to new image at position 0/0
            g.drawImage(qrImage, 0, 0, null);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

            addOverlayImage(g, qrImage, imagePath, overlayDimensions);

            ImageIO.write(combined, "png", outPath.toFile());

        } catch (IOException io) {
            throw new IllegalStateException("Could not write QR code to " + outPath, io);
        } catch (WriterException e) {
            throw new IllegalStateException("Could not write QR code text", e);
        }
    }

    private static BufferedImage addOverlayImage(Graphics2D g, BufferedImage qrImage,
                                                 URI imagePath, Dimension dimensions) {

        ImageReadParam param = new SVGReadParam();
        param.setSourceRenderSize(new Dimension(400, 400));

        // Load logo image
        BufferedImage overlay;
        try {
            overlay = readImage(imagePath, dimensions);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read overlay image from " + imagePath, e);
        }

        // Calculate the delta height and width between QR code and the logo
        // Note that we don't do any scaling, so the sizes need to kind of
        // work together without obscuring too much logo
        int deltaHeight = qrImage.getHeight() - overlay.getHeight();
        int deltaWidth = qrImage.getWidth() - overlay.getWidth();

        int woffset = Math.round(deltaWidth / 2);
        int hoffset = Math.round(deltaHeight / 2);

        // Write the logo into the combined image at position (deltaWidth / 2) and
        // (deltaHeight / 2), so that it's centered
        g.drawImage(overlay, woffset, hoffset, null);
        return overlay;
    }

    private static BufferedImage readImage(URI imagePath, Dimension dimensions) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(imagePath.toURL().openStream())) {
            // Get the reader
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                throw new IllegalArgumentException("No reader for: " + imagePath);
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(input);

                ImageReadParam param = reader.getDefaultReadParam();

                // scale svg when reading
                if (dimensions != null && "svg".equals(reader.getFormatName())) {
                    param.setSourceRenderSize(dimensions);
                }

                BufferedImage image = reader.read(0, param);

                // scale non-svg by resampling
                if (dimensions != null && !"svg".equals(reader.getFormatName())) {
                    BufferedImageOp resampler = new ResampleOp(
                            dimensions.width, dimensions.height,
                            ResampleOp.FILTER_LANCZOS);
                    image = resampler.filter(image, null);
                }

                return image;
            } finally {
                reader.dispose();
            }
        }
    }
}
