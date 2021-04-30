FROM quay.io/wsiqueir/dashbuilder-runtime:latest

# adds default admin user
RUN /opt/jboss/wildfly/bin/add-user.sh -a -u 'admin' -p 'admin' -g 'admin' 

ENV LANG en_US.UTF-8

# run as root to avoid file permission issues
USER root

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-Ddashbuilder.runtime.multi=true", "-Ddashbuilder.dev=true", "-Dfile.encoding=UTF-8" ]