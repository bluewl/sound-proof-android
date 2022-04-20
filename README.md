## Sound Proof 2FA
The Sound-Proof concept is a new 2-factor authentication approach that focuses on convenience, usability and saving the user's time by not requiring any interaction with their phone. Instead, Sound-Proof relies on location proximity between the user’s computer and phone in order to verify their legitimacy by comparing the similarity of  the ambient sound recorded by both device’s microphones.

Current prototype requires for the mobile application to be opened while authenticating so our next goal is to implement push notification to make the Sound Proof 2FA system to be completely hands-free.

## Implementation
**Cryptography** is implemented to encrypt and decrypt sensitive data.
- Asymmetrical key generation by the mobile application.
- RSA decryption and AES decryption implemented to decrypt encrypted data received from the server.

**Sound Recording** is implemented when the mobile application receives a record signal from the server.
- The mobile application records the ambient sound for 3 seconds in .wav format which is then used to compare with the browser’s recorded audio in later process.

**Sound Synchronization** is implemented to reduce and calculate lag between the two separate recording from user's PC and mobile phone.
- Makes a call to our server’s dedicated time server.
- Receives browser’s recording start time, and compares it to the phone’s start time to calculate the lag and synchronize the sound clips.

**Sound Processing** is implemented to compare different range of audible frequency to split and compare browser audio signals and mobile application audio signals to confirm co-location of two devices.
- Reads in .wav files into a raw PCM data array and split it into 24 audible range of frequencies using one third octave band filters.
- Sound comparison is done by cross-correlation for each bands which are normalized to calculate the maximum cross-correlation based on given lag value.
- Similarity score between the two audio samples is calculated by averaging the maximum cross-correlation for each bands.

## One Time Connection
Before the Sound Proof 2FA process, you will need to make a web account and connect (one time) your mobile application to that account.
1. Register an account [here](https://soundproof.azurewebsites.net/login) and log in.
2. After logging in, click on the "2FA Setup" hyperlink on the website and it should direct you to the QR code page.
3. While you are on the QR code page on the website, open your Sound Proof mobile application and go to the Connect page through the hamburger menu.
4. As shown below, click on the QR CODE button and face your camera to the QR code on the website. It will automatically connect your web account with your mobile application.
![Connect](https://user-images.githubusercontent.com/32169490/164156375-95b61441-f4fa-4c18-9365-b65a66c88af7.png)

5. Your setup for the Sound Proof 2FA is now complete. Follow the below instruction on the 2FA process.

## Sound Proof 2FA Process
After connecting your web account with your mobile application, we are now set to log in using Sound Proof 2FA:
1. After your successful enrollment, redirect your mobile application to the home page where it will say “Waiting for start record signal…”.
2. Make sure to log out from your web application.
3. While you are still on the home page of your mobile application, log in to your account through the website.
4. A toast message on the mobile phone that says “Recording Has Started” will pop up while the website will have a recording animation playing on the screen. While it is recording for 3 seconds, make sure to talk or make sounds for the best result.
![auth_process](https://user-images.githubusercontent.com/32169490/164157555-e7c3069d-3720-4c9c-99cd-5db471b53183.png)

5. A result message will display on your home screen, either successful or failed. Regardless of what results you get, you can always try to log in to the website again right away and the authentication process will start immediately.
![auth_result](https://user-images.githubusercontent.com/32169490/164157722-10c7381b-65ab-41f3-8fbd-350e6d2c6d1f.png)

## Important Links
The idea of the Sound Proof is based off from [this paper](https://www.usenix.org/conference/usenixsecurity15/technical-sessions/presentation/karapanos).

The repository for the web/server side of the Sound Proof system can be found [here](https://github.com/wilsonhammell/sound-proof-web).
