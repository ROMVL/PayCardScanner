//
//  Enums.h
//  CardRecognizer
//
//  Created by Vladimir Tchernitski on 16/03/16.
//  Copyright © 2016 Vladimir Tchernitski. All rights reserved.
//

#ifndef Enums_h
#define Enums_h

typedef enum PayCardScannerRecognizerOrientation {
    PayCardScannerRecognizerOrientationUnknown = 0,
    PayCardScannerRecognizerOrientationPortrait = 1,
    PayCardScannerRecognizerOrientationPortraitUpsideDown = 2,
    PayCardScannerRecognizerOrientationLandscapeRight = 3,
    PayCardScannerRecognizerOrientationLandscapeLeft = 4
} PayCardScannerRecognizerOrientation;

typedef enum PayCardScannerRecognizerMode {
    PayCardScannerRecognizerModeNone = 0,
    PayCardScannerRecognizerModeNumber = 1,
    PayCardScannerRecognizerModeDate = 2,
    PayCardScannerRecognizerModeName = 4,
    PayCardScannerRecognizerModeGrabCardImage = 8
} PayCardScannerRecognizerMode;


#endif /* Enums_h */
