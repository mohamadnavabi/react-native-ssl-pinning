import { NativeModules, Platform } from 'react-native';
import type {
  ErrorResponse,
  Options,
  SuccessResponse,
} from '@navabi/react-native-ssl-pinning';
import { jsonParse } from './helpers';

const LINKING_ERROR =
  `The package '@navabi/react-native-ssl-pinning' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const Sslpinning = NativeModules.Sslpinning
  ? NativeModules.Sslpinning
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export function fetch(
  url: string,
  options: Options
): Promise<SuccessResponse | ErrorResponse> {
  return new Promise((resolve, reject) => {
    Sslpinning.fetch(
      url,
      options,
      (result: SuccessResponse, error: ErrorResponse) => {
        if (error === null) {
          result.json = () => jsonParse(result.response);

          resolve(result);
        } else {
          let errorJson = jsonParse(error.error);

          const objectError = {
            error: error.error,
            json: () => errorJson,
            path: errorJson?.path,
            message: errorJson?.message,
            code: errorJson?.code,
            status: error?.status,
            url: error?.url,
          };

          reject(objectError);
        }
      }
    );
  });
}

export default Sslpinning;
