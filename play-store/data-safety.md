# Data safety form: suggested answers

Play Console, Policy and programs, App content, Data safety. These answers follow what the code does as of version 6.0.1. Read each question in Play Console: Google's own definitions decide, and you are the one declaring.

## Overview

- Does your app collect or share any of the required user data types? **Yes.** Data goes off the device to BoardGameGeek, and optionally to Google Sheets, Drive and the Gemini API.
- Is all of the user data collected by your app encrypted in transit? **Yes.** Every connection uses HTTPS.
- Do you provide a way for users to request that their data is deleted? **Yes.** Everything is on the device: uninstall or clear the app's data. Data in BGG and Google accounts is deleted there.

## Data types

Mark these as **collected**, **not shared** (each transfer is an action the user starts, toward a service they connected themselves), processed **ephemerally: no**, and collection **optional** unless noted.

| Data type | Why | Purpose in the form |
| --- | --- | --- |
| Personal info: Name | Player names in plays sent to BGG and to Gemini chronicles | App functionality |
| Personal info: User IDs | BGG username, Google account email | App functionality, Account management |
| Photos and videos: Photos | Score sheet photos sent to Gemini (optional feature) | App functionality |
| App activity: Other user-generated content | Plays, notes, moods and quotes posted to BGG or written to Sheets | App functionality |
| App info and performance | Not collected (no crash reporting or analytics) | none |
| Location | Not collected (the location field is free text you type) | none |

Score sheet photos: answer "processed ephemerally" **yes** if Play asks per type. They are sent for reading and not stored.

## Security practices

- Data encrypted in transit: Yes
- Users can request deletion: Yes (as above)
- Committed to Play Families Policy: No (not a children's app)
- Independent security review: No
