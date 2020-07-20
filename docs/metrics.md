Here summarizes skylb-specific metrics for skylb server, and skylbweb.


SkyLB Server:

| Help                                                                            | Name                                     |
|---------------------------------------------------------------------------------|------------------------------------------|
| SkyLB add observer gauge.                                                       | infra\_skylb\_add\_observer\_gauge       |
| SkyLB observer rpc counts.                                                      | infra\_skylb\_observe\_rpc\_counts       |
| SkyLB remove observer gauge.                                                    | infra\_skylb\_remove\_observer\_gauge    |
| SkyLB report load counts.                                                       | infra\_skylb\_report\_load\_counts       |
| SkyLB report load rpc counts.                                                   | infra\_skylb\_report\_load\_rpc\_counts  |
| Total number of RPCs completed on the server, regardless of success or failure. | skylb\_server\_handled\_total            |
| Total number of RPC stream messages received on the server.                     | skylb\_server\_msg\_received\_total      |
| Total number of gRPC stream messages sent by the server.                        | skylb\_server\_msg\_sent\_total          |
| Total number of RPCs started on the server.                                     | skylb\_server\_started\_total            |


SkyLB Web:

| Help                                                                                         | Name                                     |
|----------------------------------------------------------------------------------------------|------------------------------------------|
| Total number of RPCs completed by the client, regardless of success or failure.              | skylb\_client\_handled\_total            |
| Histogram of response latency (seconds) of the gRPC until it is finished by the application. | skylb\_client\_handling\_seconds         |
| Total number of gRPC stream messages sent by the client.                                     | skylb\_client\_msg\_sent\_total          |
| Number of service endpoints.                                                                 | skylb\_client\_service\_endpoint\_gauge  |
| Total number of RPCs started on the client.                                                  | skylb\_client\_started\_total            |
| Service health status.                                                                       | skylb\_web\_service\_health\_gauge       |
| The timestamp of last successful service health check.                                       | skylb\_web\_service\_health\_timestamp   |
