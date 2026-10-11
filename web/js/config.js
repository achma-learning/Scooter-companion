// Where the "Get the Android app" links point. The release workflow attaches the APK to every
// GitHub Release under this exact file name, so /releases/latest/download/... always serves
// the newest build. Set to '' to hide the links (e.g. if the repository is private).
export const APK_URL = 'https://github.com/achma-learning/kymco-agility-moto/releases/latest/download/agility-companion.apk';

// "Today's price" in the fill-up form. Published weekly on the `data` branch by
// .github/workflows/fuel-price.yml; fetched only when the owner taps the button.
export const FUEL_PRICE_URL = 'https://raw.githubusercontent.com/achma-learning/kymco-agility-moto/data/fuel-price.json';
