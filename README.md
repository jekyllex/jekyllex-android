<div align="center">
<h1>JekyllEx</h1>

<a href="https://github.com/jekyllex/jekyllex-android/blob/main/LICENSE" target="blank">
    <img src="https://img.shields.io/github/license/jekyllex/jekyllex-android" alt="JekyllEx Android App Licence" />
</a>
<a href="https://github.com/jekyllex/jekyllex-android/fork" target="blank">
    <img src="https://img.shields.io/github/forks/jekyllex/jekyllex-android" alt="JekyllEx Android App Forks"/>
</a>
<a href="https://github.com/jekyllex/jekyllex-android/stargazers" target="blank">
    <img src="https://img.shields.io/github/stars/jekyllex/jekyllex-android" alt="JekyllEx Android App Stars"/>
</a>
<a href="https://github.com/jekyllex/jekyllex-android/issues" target="blank">
    <img src="https://img.shields.io/github/issues/jekyllex/jekyllex-android" alt="JekyllEx Android App Issues"/>
</a>
</div>

<div align="center">
    <sub>Built with ❤︎ by
        <a href="https://github.com/gouravkhunger">Gourav Khunger</a>
    </sub>
</div>
<br/>

<img alt = "JekyllEx Introduction Image" src="https://raw.githubusercontent.com/jekyllex/jekyllex-android/main/media/cover-image.jpg"/>

# 🚀 Introduction

<img alt = "JekyllEx App Logo" src="https://raw.githubusercontent.com/jekyllex/jekyllex-android/main/media/logo.jpg" height="100" width="100" align="right" style="margin:10px"/>

JekyllEx is an Android App that allows you to manage a Jekyll Blog directly from your Android device!

[Read the blog post for better understanding](https://genicsblog.com/introducing-jekyllex-android-app) 😃.

# ✨ Try it out

Download and install the [latest release](https://github.com/jekyllex/jekyllex-android/releases/latest) of the app to
start relishing the power of blogging from your mobile.

# 🛠️ Technical details

The codebase of this app is based on the MVVM pattern.

Here's a list of tools/libraries/components JekyllEx uses.

### Platform

- Android

### Languages Used

- Kotlin
- XML

### Libraries

- Android Architecture Components
- [Auth0](https://auth0.com/) : For user authentication
- [Retrofit](https://github.com/square/retrofit) : For network requests
- [Room](https://developer.android.com/training/data-storage/room) : For local database and caching user profile
- Kotlin extensions and Coroutines for Room
- [Markwon](https://github.com/noties/Markwon) : Markdown rendering
- [App Updater](https://github.com/javiersantos/AppUpdater) : To check for updates from GitHub Releases
- [Glide](https://github.com/bumptech/glide) : For image loading.
- [Firebase](https://firebase.google.com/) : For push notifications, analytics and crashlytics.

# ⚙ Local Setup

To build this app on your local machine:

1. Clone the repository

   ```
   git clone https://github.com/jekyllex/jekyllex-android.git
   ```

2. Make `gradle.properties` file in the cloned folder with the following content:

   ```properties
   android.useAndroidX=true
   android.enableJetifier=true

   Auth0ClientId="enter Auth0 client ID here"
   API_AUDIENCE="enter custom API audience link here"
   ```

   Other properties can vary from machine to machine, the required parameters are `Auth0ClientId` AND `API_AUDIENCE`


3. Make a firebase project and add connect the project with it.


4. You're done with setting up local development!

# 👨‍💻 Author

**Gourav Khunger** - [Learn more about me](https://github.com/gouravkhunger).

# 😄 Support

Please give this project a ⭐ to motivate me for improving JekyllEx!

Consider donating some coffee if you wish to support me to make more open source projects ;)

<div align="center">
   <a href='https://ko-fi.com/E1E21Q5FY' target='_blank'>
      <img height='50' src='https://cdn.ko-fi.com/cdn/kofi5.png?v=3' alt='Buy Gourav Khunger a Coffee at ko-fi.com' />
   </a>
</div>

# 🛡 License

This project is [`MIT`](https://github.com/jekyllex/jekyllex-android/blob/main/LICENSE) Licensed.

```
MIT License

Copyright (c) 2021 Gourav Khunger

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<div align="center">
JekyllEx needs a ⭐ from you =)
</div>
