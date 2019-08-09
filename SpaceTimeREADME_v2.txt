SpaceTime: Video Chat Application for Android Users

This application allows users to video chat on android devices through a 
shared, user created access code. 
____________________________
Installation:
____________________________
-Install the SpaceTime APK to your device by copying the spacetime.apk file
to an accessible folder on your phone(such as documents). 
-Once the APK is on your device, locate it using your File Manager and 
select it. 
-Bypass security warnings such as, unknown source restriction and 
Google Play advisory, and install the app.


____________________________
Use:
____________________________
-Upon initializaiton the menu will prompt the user to enter a room name.
Once a name is entered and chosen, if they are the first to enter, they will
be taken to a video room with the front camera active.
	*The first time a room is entered, you will be prompted for app 
	permissions to the camera and microphone

-When the other user enters the room, your preview camera will move to the 
bottom of the screen, and their image will take the forefront.
-While in the chat, there are buttons to end the call, swithch camera,
expand the camera, and mute the microphone.

-In the main menu, there is an option to add room names to a list of 
favorites for quick access in the future.
	-To remove a favorite, hold down on the name in the list, until the
	'remove favorite' option appears. Select it and the name will be
	 removed.

There is also a loopback option that allows you to test call conditions with
yourself


____________________________
Server Discussion:
____________________________
A server runs independent of the devices created through Ubuntu and running
off of my laptop. Each device is connected to a network that will give a 
private IP address to the device that differentiates it from other devices 
on the network. 

When the application makes a call to the server, it sends both the 
identifier(room name) for the room and the IP address of the initial device. 
When the other device enters the same identifier and makes a call, 
the identifier and the IP address are sent. Once the identifier is 
found to match, the two devices send video and audio data through 
their network connection. 

The room name is crutial, as use of a different name would be recognized 
the creation of a new connection. Room names cannot be searched for 
within SpaceTime.

Each room is only meant to hold two individuals. If another user attempts
to enter a room that already contains two people, an error message is 
given and the user is returned to the home screen. 		
