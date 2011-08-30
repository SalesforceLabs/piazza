echo "NOTE: Starting to generate certificates.  You are free to press enter the entire way through."
openssl genrsa 1024 > conf/host.key
openssl req -new -x509 -nodes -sha1 -days 365 -key conf/host.key > conf/host.cert
echo "DONE!"
