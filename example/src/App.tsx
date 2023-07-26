import * as React from 'react';

import { StyleSheet, View, Text } from 'react-native';
import { fetch as SSLPinningFetch } from '@navabi/react-native-ssl-pinning';

export default function App() {
  const [result, setResult] = React.useState({});

  React.useEffect(() => {
    (async () => {
      const response = await SSLPinningFetch(`URL`, {
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
      setResult(resultJson);
    })();
  }, []);

  return (
    <View style={styles.container}>
      <Text>Result: {result}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
