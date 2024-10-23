[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.vlasentiy/lens24/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.vlasentiy/lens24)
[![API](https://img.shields.io/badge/API-16%2B-blue.svg?style=flat)](https://android-arsenal.com/api?level=16)
<a href="https://github.com/ROMVL/PayCardScanner/blob/master/LICENSE.md">
    <img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="Lens24 is released under the MIT license." />
  </a>

Lens24 is SDK for Android that gives you ability to scan various of credit or payment cards in your app offline.
You can easily integrate and customize the SDK into your app by following the instructions below.

### SDK integration

In your `build.gradle`, add maven repository to repositories list

```
    repositories {
        mavenCentral()
    }
```

<br />
Add _Lens24_ as a dependency
```
dependencies {
    implementation 'io.github.romvl:paycardscanner:1.0.0'
}
```

### Usage

Build an Intent using the `ScanCardIntent.Builder` and start a new activity to perform the scan:

#### Kotlin

```kotlin
class MyActivity : AppCompatActivity {

    private var activityResultCallback = ScanCardCallback.Builder()
        .setOnSuccess { card: Card, bitmap: Bitmap? -> setCard(card, bitmap) }
        .setOnBackPressed { /*Your code here*/ }
        .setOnManualInput { /*Your code here*/ }
        .setOnError { /*Your code here*/ }
        .build()

    private var startActivityIntent = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult(),
        activityResultCallback
    )

    private fun setCard(card: Card, bitmap: Bitmap?) {
        /*Your code here*/
    }

    private fun scanCard() {
        val intent: Intent = ScanCardIntent.Builder(this)
            // customize these values to suit your needs
            .setScanCardHolder(true) // version [1.0.0..2.0.0)
            .setScanExpirationDate(true) // version [1.0.0..2.0.0)
            .setVibrationEnabled(false)
            .setHint(getString(R.string.hint))
            .setToolbarTitle("Scan card")
            .setSaveCard(true)
            .setManualInputButtonText("Manual input")
            .setBottomHint("bottom hint")
            .setMainColor(R.color.primary_color_dark)
            .setLottieJsonAnimation("lottie json data") //// version [1.0.0..2.0.0)
            .build()

        startActivityIntent.launch(intent)
    }
}
```

### License

```
MIT License

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
