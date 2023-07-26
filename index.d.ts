declare module '@navabi/react-native-ssl-pinning' {
  interface Response {
    status: number;
    url: string;
    json: () => Promise<{ [key: string]: any }>;
  }

  export interface SuccessResponse extends Response {
    response: string;
    responseJSON: Promise<{ [key: string]: any }>;
  }

  export interface ErrorResponse extends Response {
    error: string;
    path: string;
    message: string;
    code: string;
  }

  interface Header {
    [headerName: string]: string;
  }

  export interface Options {
    body?: string | object;
    headers?: Header;
    method?: 'DELETE' | 'GET' | 'POST' | 'PUT';
    certificates?: string[];
    validDomains?: string[];
    timeout?: number;
  }

  export async function fetch(
    url: string,
    options: Options
  ): Promise<SuccessResponse | ErrorResponse>;

  export default Sslpinning;
}
