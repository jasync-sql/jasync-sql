# Authentication methods

This driver implements multiple authentication methods available in MySQL/MariaDB and PostgresSQL.
The step-by-step authentication flow and implementation details are described below.

## `caching_sha2_password`

This is the default authentication method since MySQL 8.0.
Official documentation can be found [here][caching-sha2-password].

The fast authentication flow (using password scrambling) is as follows:
1. During the handshake, MySQL server sends the authentication seed (nonce).
2. The driver scrambles the password using SHA-256 with `AuthenticationScrambler`, and sends `HandshakeResponse`.
3. If the password entry is cached on the server, it performs fast authentication, and returns `AuthMoreData`
   message indicating success (`data=3`). This is followed by `OkMessage` and the authentication flow completes.
4. In case the password is not cached, the server requires us to switch to full authentication, and returns
   `AuthMoreData` with `data=4`.

If we need to perform the full authentication flow (using SHA-256 hashing), the process is as follows:
1. If we're connected over SSL, we can send `AuthenticationSwitchResponse` with a plaintext password.
   Note that if we try to do the same over an unsafe connection, the server always rejects the password.
2. If we are not connected over SSL, we can use the provided `rsaPublicKey` (used by the server) to encrypt the
   password, and send it as `AuthenticationSwitchResponse`. See `Sha256PasswordAuthentication` for
   implementation details.
3. If `rsaPublicKey` is not specified, the public key used to encrypt the password can be fetched from the
   server. **This is currently not supported by the driver.**
4. If the authentication was successful, the server caches the password entry, and returns `OkMessage`.
   The next authentication request for the specified user can therefore be done with fast authentication.

## `sha256_password`

This authentication method has been deprecated in favor of `caching_sha2_password` in MySQL 8.0, and works the
same as its full authentication flow.

## `mysql_native_password`

This was the default authentication method until MySQL 8.0.
Official documentation can be found [here][mysql-native-password].

The authentication flow is as follows:
1. During the handshake, MySQL server sends the authentication seed (nonce).
2. The driver scrambles the password using SHA-1 with `AuthenticationScrambler`, and sends `HandshakeResponse`.
3. If the password is correct the server sends `OkMessage`, and `ErrorMessage` otherwise.

## `mysql_old_password`

This method was mainly used before MySQL 4.1. It was deprecated in MySQL 5.6 and removed in MySQL 5.7.
Official documentation can be found [here][mysql-old-password].

The authentication flow is as follows:
1. During the handshake, MySQL server sends the authentication seed (nonce). This can either be 8 bytes on older
   versions of MySQL, or 20 bytes if the server uses the `mysql_native_password` method as the default. In the
   latter case, the driver uses the first 8 bytes of the seed.
2. The driver hashes the password using a proprietary algorithm, and sends `HandshakeResponse`. See
   `OldPasswordAuthentication` for implementation details.
3. If the password is correct the server sends `OkMessage`, and `ErrorMessage` otherwise.

[caching-sha2-password]: https://dev.mysql.com/doc/dev/mysql-server/8.0.32/page_caching_sha2_authentication_exchanges.html
[mysql-native-password]: https://dev.mysql.com/doc/dev/mysql-server/8.0.32/page_protocol_connection_phase_authentication_methods_native_password_authentication.html
[mysql-old-password]: https://dev.mysql.com/doc/dev/mysql-server/8.0.32/page_protocol_connection_phase_authentication_methods.html#page_protocol_connection_phase_authentication_methods_old_password_authentication
