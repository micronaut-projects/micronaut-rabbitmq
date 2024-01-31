These long-lived certificates were created to test the SSL, they are in no way secure.

To create them, [I followed the instructions here](https://www.rabbitmq.com/ssl.html#automated-certificate-generation-transcript) and [here](https://github.com/rabbitmq/tls-gen):

Which are:

```
git clone https://github.com/rabbitmq/tls-gen tls-gen
```

then:

```
cd tls-gen/basic
```

then:

```
# pass a private key password using the PASSWORD variable if needed
make

## copy or move files to use hostname-neutral filenames
## such as client_certificate.pem and client_key.pem,
## this step is optional
make alias-leaf-artifacts
```

with output in

```
# results will be under the ./result directory
ls -lha ./result
```
