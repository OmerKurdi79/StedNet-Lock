# Privacy Policy for StedNet-Lock

**Effective Date:** June 14, 2026

`StedNet-Lock` is built as an open-source utility application. This privacy policy describes how your information is handled when using this application.

---

## 1. Information Collection and Use

**StedNet-Lock is an offline-first application.** 
- **No Personal Data Collection**: We do not collect, monitor, track, or share any personally identifiable information (PII) or usage analytics.
- **Local Storage**: Any configurations, SIM subscriptions, or custom network profiles you create are stored locally on your device in a secure private database (Room Database). This data never leaves your device.
- **No Network Transmission**: The app does not send any of your device configurations or profile settings to any external servers.

---

## 2. Permissions Required and Why

To function correctly, the app requests the following system permissions on your device:

- **Shizuku API (`rikka.shizuku.permission.API_V23`)**: Used to execute system-level network configuration commands securely without requiring root access.
- **Read Phone State (`android.permission.READ_PHONE_STATE`)**: Used exclusively to read active SIM card configurations and subscription IDs. This is required so that the app can apply your customized network profiles to the correct active SIM card.
- **Access Network State (`android.permission.ACCESS_NETWORK_STATE`)**: Used to detect whether the device is connected to a network to display the current state accurately.

---

## 3. Third-Party Services

The application **does not** use or integrate any third-party services, including:
- No advertising networks (No Ads)
- No tracking SDKs or telemetry (No Analytics)
- No cloud databases or external hosting

---

## 4. Children’s Privacy

Since the application does not collect any data from its users, it does not knowingly collect or solicit any information from children under the age of 13. 

---

## 5. Changes to This Privacy Policy

We may update our Privacy Policy from time to time. Since the project is hosted on GitHub, any updates to this policy will be transparently documented and visible in the repository's commit history. 

---

## 6. Contact Us

If you have any questions or feedback about this Privacy Policy, please open an issue or contact us through the repository's GitHub issues page:
[StedNet-Lock GitHub Issues](https://github.com/OmerKurdi79/StedNet-Lock/issues)
