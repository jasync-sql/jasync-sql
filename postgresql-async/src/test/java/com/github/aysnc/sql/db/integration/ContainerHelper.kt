package com.github.aysnc.sql.db.integration

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import mu.KotlinLogging
import org.testcontainers.containers.PostgreSQLContainer
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

private val version = "9.3"

/**
 * See run-docker-postresql.sh to run a local instance of postgreSql.
 */
object ContainerHelper {
    var postresql: MyPostgreSQLContainer? = null

    val port: Int
        get() = defaultConfiguration.port

    /**
     * default config is a local instance already running on port 15432 (i.e. a docker postresql)
     */
    var defaultConfiguration = Configuration(
        "postresql_async",
        "localhost",
        15432,
        "root",
        "netty_driver_test"
    )

    init {
        try {
            PostgreSQLConnection(defaultConfiguration).connect().get(1, TimeUnit.SECONDS)
            logger.info("Using local postresql instance $defaultConfiguration")
        } catch (e: Exception) {
            // If local instance isn't running, start a docker postresql on random port
            if (postresql == null) {
                configurePostgres()
            }
            if (!postresql!!.isRunning()) {
                postresql!!.start()
            }
            defaultConfiguration = Configuration(
                postresql!!.getUsername(),
                "localhost",
                postresql!!.getFirstMappedPort()!!,
                postresql!!.getPassword(),
                postresql!!.getDatabaseName()
            )
            logger.info("PORT is " + defaultConfiguration.port)
            logger.info("Using test container instance {}", defaultConfiguration)
        } finally {
            try {
                val connection = PostgreSQLConnection(defaultConfiguration).connect().get(1, TimeUnit.SECONDS)
                logger.info("got connection " + connection.isConnected())
                //logger.info("select 1: " + connection.sendQuery("select 1").get().rowsAffected)
                connection.sendQuery(
                    """
          DROP TYPE IF EXISTS example_mood; CREATE TYPE example_mood AS ENUM ('sad', 'ok', 'happy')
        """
                ).get()
                connection.sendQuery(
                    """
          CREATE USER postgres_cleartext WITH PASSWORD 'postgres_cleartext'; GRANT ALL PRIVILEGES ON DATABASE ${defaultConfiguration.database} to postgres_cleartext;
          CREATE USER postgres_md5 WITH PASSWORD 'postgres_md5'; GRANT ALL PRIVILEGES ON DATABASE ${defaultConfiguration.database} to postgres_md5;
          CREATE USER postgres_kerberos WITH PASSWORD 'postgres_kerberos'; GRANT ALL PRIVILEGES ON DATABASE ${defaultConfiguration.database} to postgres_kerberos;
        """
                ).get()
            } catch (e: Exception) {
                logger.error(e.localizedMessage, e)
            }

        }
    }

    private fun configurePostgres() {

        val PGCONF = "/etc/postgresql/$version/main"
        val hba = """
  local    all             all                                     trust
  host     all             postgres           127.0.0.1/32         trust
  host     all             postgres_md5       127.0.0.1/32         md5
  host     all             postgres_cleartext 127.0.0.1/32         password
  host     all             postgres_kerberos  127.0.0.1/32         krb5
          """.trimIndent()
        val serverCrt = """
            Certificate:
      Data:
          Version: 3 (0x2)
          Serial Number: 9913731310682600948 (0x8994a61a13e775f4)
      Signature Algorithm: sha1WithRSAEncryption
          Issuer: CN=localhost
          Validity
              Not Before: Mar  6 08:12:28 2016 GMT
              Not After : Apr  5 08:12:28 2016 GMT
          Subject: CN=localhost
          Subject Public Key Info:
              Public Key Algorithm: rsaEncryption
                  Public-Key: (2048 bit)
                  Modulus:
                      00:ce:26:60:f9:0d:0f:f1:d6:ed:3e:79:91:55:6a:
                      18:63:23:96:f2:60:50:3d:e3:dd:72:e8:c2:54:17:
                      50:be:f0:9c:32:95:39:75:b1:04:a7:bb:f5:10:a4:
                      eb:d0:10:e2:17:45:d3:f9:35:8e:b4:8f:14:97:8f:
                      27:93:d7:20:05:e2:dc:68:64:bc:fd:f2:19:17:94:
                      e8:2f:a6:b2:54:3f:df:3e:e7:8f:f1:52:15:7a:30:
                      81:4d:bb:6f:22:8c:ca:e1:cb:6a:72:6d:fa:89:50:
                      e7:ee:07:d1:84:8a:71:07:dc:3f:6f:1f:db:10:e9:
                      93:ad:01:c5:2b:51:ce:58:ef:12:95:00:16:e8:d4:
                      46:07:35:ee:10:47:c4:f7:ff:47:17:52:a5:bb:5c:
                      cb:3c:f6:6b:c8:e7:d9:7c:18:39:a1:8f:e0:45:82:
                      88:b5:27:f3:58:cb:ba:30:c0:8a:77:5b:00:bf:09:
                      10:b1:ad:aa:f4:1b:2c:a1:f9:a5:59:57:c8:ef:de:
                      54:ad:35:af:67:7e:29:bc:9a:2a:d2:f0:b1:9c:34:
                      3c:bc:64:c9:4c:93:2c:7d:29:f4:1a:ac:f3:44:42:
                      a4:c9:06:1e:a4:73:e6:aa:67:d0:e4:02:02:ba:51:
                      1e:97:44:b8:4b:4e:55:cd:e6:24:49:08:ac:9b:09:
                      19:31
                  Exponent: 65537 (0x10001)
          X509v3 extensions:
              X509v3 Subject Key Identifier:
                  2E:20:4D:E1:12:2A:B0:6F:52:7F:62:90:D4:78:7B:E3:7D:D5:60:10
              X509v3 Authority Key Identifier:
                  keyid:2E:20:4D:E1:12:2A:B0:6F:52:7F:62:90:D4:78:7B:E3:7D:D5:60:10

              X509v3 Basic Constraints:
                  CA:TRUE
      Signature Algorithm: sha1WithRSAEncryption
           9b:e8:50:8b:86:0f:bf:22:c6:b4:ef:3e:c9:a2:55:fb:69:fc:
           ae:93:7b:5e:6a:b6:ed:5b:27:c2:9e:36:d6:f1:f1:0f:67:65:
           87:de:05:21:6e:0e:f4:df:ac:72:61:47:f8:fd:16:9b:3d:54:
           ef:21:cf:b7:31:ba:bf:c9:1b:2c:a0:f9:f1:6b:45:5a:98:25:
           b9:01:99:cf:e1:79:c5:6a:20:ce:ca:ca:3f:6d:56:f3:65:51:
           31:98:01:b9:96:99:04:9c:ab:ae:fb:3f:f8:ad:60:66:77:54:
           b2:81:e3:7c:6b:c4:36:ae:ae:5c:c6:1a:09:5c:d6:13:da:2b:
           ba:ef:3f:3e:b2:13:f2:51:15:c5:1b:9c:22:be:b4:55:9b:15:
           70:60:3d:98:6e:ef:53:4c:c7:20:60:3f:17:f3:cc:76:47:96:
           27:05:84:0e:db:21:e1:76:b7:9c:38:35:19:ef:52:d4:fc:bd:
           ec:95:2e:eb:4b:5b:0b:c8:86:d7:23:c2:76:14:f3:93:6f:c0:
           a9:b6:ca:f8:47:3e:9d:af:11:5d:73:79:68:70:26:f9:fd:39:
           60:c1:c3:c7:a9:fc:48:b5:c0:e6:b4:2e:07:de:6a:ca:ed:04:
           67:31:b8:0b:d0:48:fd:3b:4c:12:8a:34:5c:18:3f:38:85:f2:
           1c:96:39:50
  -----BEGIN CERTIFICATE-----
  MIIC+zCCAeOgAwIBAgIJAImUphoT53X0MA0GCSqGSIb3DQEBBQUAMBQxEjAQBgNV
  BAMMCWxvY2FsaG9zdDAeFw0xNjAzMDYwODEyMjhaFw0xNjA0MDUwODEyMjhaMBQx
  EjAQBgNVBAMMCWxvY2FsaG9zdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoC
  ggEBAM4mYPkND/HW7T55kVVqGGMjlvJgUD3j3XLowlQXUL7wnDKVOXWxBKe79RCk
  69AQ4hdF0/k1jrSPFJePJ5PXIAXi3GhkvP3yGReU6C+mslQ/3z7nj/FSFXowgU27
  byKMyuHLanJt+olQ5+4H0YSKcQfcP28f2xDpk60BxStRzljvEpUAFujURgc17hBH
  xPf/RxdSpbtcyzz2a8jn2XwYOaGP4EWCiLUn81jLujDAindbAL8JELGtqvQbLKH5
  pVlXyO/eVK01r2d+KbyaKtLwsZw0PLxkyUyTLH0p9Bqs80RCpMkGHqRz5qpn0OQC
  ArpRHpdEuEtOVc3mJEkIrJsJGTECAwEAAaNQME4wHQYDVR0OBBYEFC4gTeESKrBv
  Un9ikNR4e+N91WAQMB8GA1UdIwQYMBaAFC4gTeESKrBvUn9ikNR4e+N91WAQMAwG
  A1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADggEBAJvoUIuGD78ixrTvPsmiVftp
  /K6Te15qtu1bJ8KeNtbx8Q9nZYfeBSFuDvTfrHJhR/j9Fps9VO8hz7cxur/JGyyg
  +fFrRVqYJbkBmc/hecVqIM7Kyj9tVvNlUTGYAbmWmQScq677P/itYGZ3VLKB43xr
  xDaurlzGGglc1hPaK7rvPz6yE/JRFcUbnCK+tFWbFXBgPZhu71NMxyBgPxfzzHZH
  licFhA7bIeF2t5w4NRnvUtT8veyVLutLWwvIhtcjwnYU85NvwKm2yvhHPp2vEV1z
  eWhwJvn9OWDBw8ep/Ei1wOa0LgfeasrtBGcxuAvQSP07TBKKNFwYPziF8hyWOVA=
  -----END CERTIFICATE-----

          """.trimIndent()
        val serverKey = """
            -----BEGIN RSA PRIVATE KEY-----
  MIIEowIBAAKCAQEAziZg+Q0P8dbtPnmRVWoYYyOW8mBQPePdcujCVBdQvvCcMpU5
  dbEEp7v1EKTr0BDiF0XT+TWOtI8Ul48nk9cgBeLcaGS8/fIZF5ToL6ayVD/fPueP
  8VIVejCBTbtvIozK4ctqcm36iVDn7gfRhIpxB9w/bx/bEOmTrQHFK1HOWO8SlQAW
  6NRGBzXuEEfE9/9HF1Klu1zLPPZryOfZfBg5oY/gRYKItSfzWMu6MMCKd1sAvwkQ
  sa2q9BssofmlWVfI795UrTWvZ34pvJoq0vCxnDQ8vGTJTJMsfSn0GqzzREKkyQYe
  pHPmqmfQ5AICulEel0S4S05VzeYkSQismwkZMQIDAQABAoIBAH80v3Hu1X/tl8eN
  TFjgdtv2Ahbdx6XpDaTya7doC7NG1ZuA6UvuR2kZWkdC/SAOyvSBaiPFIKHaCGLd
  OxbHEEORkV/5iYVJ9qHOiNeejTvfjepLCU9nz0ju1VsZ5aH0LtzVoIGry4UgH32J
  5YdbxhOLnLj9dzggabe/9+KbQDEveGTzkIvSJ1nbts7c8IRp6t/1nBz54BhawUjJ
  IbaEbCH/mEmiCOUP914SCAUEfmgbMhdx8dc4V9nyxK+bulF3WIEpVZU1zj5Rpyni
  P8gQ1geI64Erd8oa4DJ5C77eLuKKk0JBCkgh5x3hiAxuvN0zxHxW2Q75c6x9uDr5
  DXi20GECgYEA+NRW6heYBJw7Lt7+cQCRG5/WFOX9TmmK9EAidVPULWO4NN4wLZxa
  exW/epg8w1Y+u+BHOzFq9idJaHsoLZCmoNWMkZsP+AzeEkklee6wgur3/Zs1HqHZ
  1VA3EmvOecz++3o69zcjd0nzgk9ADhjA2dAahKTnn5RESD1dFBWU2+sCgYEA1Bcv
  PiQe6ce86FlSPr0TBFvIJl2dfjrQijL3dhZMo+1Y5VTShGBoAQKfBhJITSbsmaEz
  UQ/4rBMyTN9bwvSwsDpQZw/Y0YKiSQIOr4J0jyotY5RN2AH3AlCX8CrhoOmBaLUd
  n2SGx5keodnXn1/GPkuGPIa7xnGib/gdL2AaZFMCgYBV5AX0XByPStZrAXJW01lD
  bdLZ9+GOFYRvd0vtr/gHiupk5WU/+T6KSiGEUdR3oOeatnogBpjjSwBd3lUqFUpP
  LieNgzbp6pclPLaA9lFbf3wGwHJ/lmK47S11YF0vUgGaEMEV4KSPYql5i52SwByh
  kuH0c2+4d9dyECx26FQv7QKBgQDBtX83oWP+n6hhCpu8o5IH7BAtQlmDHhKz9oLf
  /tP28OO9abBwqWC0c4Fs2SviE4gLdRjak9zKxSmu3l3//N6XxlsDFo0wJcE1L0Tc
  dikhTSNxjNVgUcMaASQUfgXfowXH7YvltboH+UjqCH4QmTgGU5KCG4jLYaQ74gA9
  8eeI8wKBgDfclcMsJnY6FpFoR0Ub9VOrdbKtD9nXSxhTSFKjrp4JM7SBN3u6NPJK
  FgKZyQxd1bX/RBioN1prrZ3rbg+9awc65KhyfwtNxiurCBZhYObhKJv7lZyjNgsT
  EALMKvB+fdpMtPZOVtUl0MbHEBblrJ+oy4TPT/kvMuCudF/5arcZ
  -----END RSA PRIVATE KEY-----

          """.trimIndent()
        val PGDATA = "/var/lib/postgresql/data"
        postresql = MyPostgreSQLContainer()
            .withDatabaseName("netty_driver_test")
            .withPassword("root")
            .withUsername("postresql_async")
            .withCommand("/bin/sh", "-c", "echo \"$hba\" > $PGCONF/pg_hba.conf")
            .withCommand("/bin/sh", "-c", "echo \"$serverCrt\" > $PGDATA/server.crt")
            .withCommand("/bin/sh", "-c", "echo \"$serverKey\" > $PGDATA/server.key")

    }

}

class MyPostgreSQLContainer : PostgreSQLContainer<MyPostgreSQLContainer>("postgres:$version")
