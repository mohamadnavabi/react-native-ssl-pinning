# react-native-ssl-pinning

ssl pinning for React Native

## Installation

```sh
npm install @navabi/react-native-ssl-pinning
```

```sh
yarn add @navabi/react-native-ssl-pinning
```

## Usage

```js
import { fetch } from '@navabi/react-native-ssl-pinning';

// ...
const response = await fetch(`URL`, {
  body: {},
  headers: {},
  certificates: [
    /* certs */
  ],
  validDomains: [
    /* your valid domain */
  ],
  timeout: 6000,
});
let resultJson = await response.json();
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
