# Rotoshokan

Auto-searching for e-books in a library using your phone camera. Point your camera at a book (ISBN, barcode, or cover), and Rotoshokan will search Google Books (or other configured sources) to quickly locate metadata and links to the book. The project can run on refurbished phones acting as lightweight servers and be accessed remotely via ngrok.

## Features
- Scan ISBN, barcode, or book cover using the camera.
- Query Google Books (or other configured APIs) to find matching e-book metadata.
- Designed to run on low-cost or refurbished phones as small servers.
- Optional remote access using ngrok so devices don't need to be on the same local network.

## Requirements
- A device with a camera (Android phone recommended for phone-server mode).
- The runtime for this project (check the repository for a package.json, build.gradle, or requirements.txt for exact requirements).
- (Optional) ngrok account and client for exposing the local server to the internet.
- Google Books API key (recommended) or other book search API credentials if configured.

## Quick start
1. Clone the repository:

   git clone https://github.com/NojuRonald/Rotoshokan.git
   cd Rotoshokan

2. Install dependencies

   npm install

   If this project uses another package manager or runtime, use the appropriate install command (yarn, pip, Gradle, etc.).

3. Configure environment

   - Create a .env or configuration file at the project root (see the repo for an example). Typical variables you might need:
     - GOOGLE_BOOKS_API_KEY=your_api_key_here
     - NGROK_AUTH_TOKEN=your_ngrok_token_here

4. Run the server (example)

   npm run start

   Or the command used by this repository (check package.json or other start scripts).

5. (Optional) Expose with ngrok

   ngrok http 3000

   Replace 3000 with the server port used by this project.

6. Open the app on the device or point your camera (or device's browser) to the server URL. If using ngrok, use the generated public URL.

## Notes
- This README is generic; if any of the commands above don't match the codebase, open the relevant files (package.json, build.gradle, or other config files) and follow the implemented start instructions.
- If you intend to run multiple phones as servers, ensure each device has a unique ngrok tunnel or port mapping.

## Contributing
Contributions are welcome. Please open issues for bugs or feature requests, and send pull requests for changes. Include a clear description of your changes and any setup required to test them.

## License
Specify a license for this project (e.g., MIT) by adding a LICENSE file to the repository.

## Contact
If you want help improving this README with project-specific commands or environment examples, tell me which files in the repo show the server startup commands (for example, package.json or MainActivity.kt) and I will update the README accordingly.
