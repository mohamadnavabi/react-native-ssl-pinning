export const jsonParse = (value: string) => {
  try {
    return JSON.parse(value);
  } catch (e) {
    return {};
  }
};
