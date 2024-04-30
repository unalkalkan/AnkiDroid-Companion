# AnkiDroid Companion

AnkiDroid Companion is an extension app designed to enhance the functionality of the AnkiDroid flashcard app. It provides convenient access to AnkiDroid's Card Practicing feature via permanent notifications without the need to open the app itself.

![](.github/output.gif)


## Features

- **Enhanced AnkiDroid Functionality**: AnkiDroid Companion allows users to access AnkiDroid's Deck practising directly from permanent notifications.
    
- **Seamless Integration**: The app seamlessly integrates with AnkiDroid, leveraging its existing features, this app does not store any information regarding the deck, card and review. Everything happens on your AnkiDroid app.
    
- **Persistent Notifications**: AnkiDroid Companion keeps the notification persistent as long as there are cards to practice on your deck. When you finish practising the deck, you can close the notification until the next one pops-up!
    
- **Future Expansion**: In future updates, we plan to extend the app's functionality by adding Android widgets for the home screen and lock screen


## Getting Started

To get started with AnkiDroid Companion, follow these steps:

1. **Install AnkiDroid**: Ensure that AnkiDroid is installed on your device. AnkiDroid Companion relies on AnkiDroid's functionality and requires it to be present on the device.
    
2. **Install AnkiDroid Companion**: Download AnkiDroid Companion from the [releases](https://github.com/unalkalkan/AnkiDroid-Companion/releases) page
    
3. **Grant Permissions**: Upon installation, ensure that AnkiDroid Companion has the necessary permissions to access notifications and interact with AnkiDroid, as well as notification permissions to display notifications.
	
4. **Enable Notifications:** Make sure to enable notifications for AnkiDroid Companion app in your Android System settings
    
5. **Start Using**: Once installed, AnkiDroid Companion will access to AnkiDroid app and display your decks. Choose a deck and click "Refresh" button. It'll show you a notification right away!
      
6. **Finishing Decks**: When you get to the end of your deck, you can either change your deck or wait for the AnkiDroid Companion to send you a new notification when it's time for you to study.


## Limitations & Improvements
  
- **Minimalistic Card Support**: Currently this app only supports simple cards with small texts that doesn't include any HTML. If your case involves complex scenarios, you can try to implement them or create an issue for someone with more time to pick up.

- **Skipping Cards**: Skipping cards was not something that came to my mind just until now, we need to came up with a way to do this since when a skip is needed its going to be a pain with the current setup.
    
- **New Study Interval**: Currently when you finish a deck, it will wait for 8 hours and check again to see if you have new cards to study. I plan to add this as a setting to the app so you can change it, but had no time so far.
	
- **Embedded Strings:** There are _so many_ embedded string inside the app. I know that one should move them to resouces/string. But had no time either.


## Contributing

Contributions to AnkiDroid Companion are welcome! If you have any ideas, feature requests, or bug reports, please feel free to open an issue or submit a pull request on GitHub.


## License

This project is licensed under the MIT License.
