# Akka Actor Ladybugs #

Once when studying at [KTH](http://www.kth.se/) we had a programming assignment consisting of building an
application where [Ladybugs](http://en.wikipedia.org/wiki/Coccinellidae) would wander around on the screen
living their life.

The hard part of that assignment was that each ladybug must be represented by its own
[thread](http://en.wikipedia.org/wiki/Thread_\(computing\)) which should handle all operations concerning the
ladybug that it represented, including drawing to the screen.
This exercise was mainly about learning to program correctly in the realms of multi-threading and the exercise
was set to be done in C++ targeting the Win32 API.

*Here follows a video of that application running:*

<iframe width="640" height="480" src="//www.youtube.com/embed/mhjmqMw9Lnc" frameborder="0" allowfullscreen></iframe>

I remember spending quite a lot of time on this assignment, not only on the programming part but also on the
graphics side where I put some extra effort modelling and animating the ladybugs in 3D-studio.
The year must have been 1999.

Ever since taking the [Coursera](https://www.coursera.org/) course
[Principles of Reactive Programming](https://www.coursera.org/course/reactive) I've had the idea to reimplement
this old school assignment using [Akka](http://akka.io/) where each ladybug would be represented by an
[Actor](http://en.wikipedia.org/wiki/Actor_model).

My idea for the end result is be to display the ladybugs wandering around in a web-browser updating the current
state in real-time through a [websocket](http://en.wikipedia.org/wiki/WebSocket).

This project is currently a work in progress. It's not feature complete compared to the original, but will be
eventually.
 
It can be seen live right now by browsing to: [http://ladybugs.herokuapp.com/](http://ladybugs.herokuapp.com/)  

                                                                                                                                                                                                                                   
## Running ##
                                                                                                                                                                                                                      
The easiest way to run this application is with the following command in the base directory:                                                                                                                                    
```
./activator run
```                                                                                                                                                                                                                                
After this it can be accessed from a web browser at **http://localhost:8080/**.

To run in development mode use:
```
./activator ~re-start
```
This will run it in such a way that it will recompile and restart upon changes thanks to
[sbt-revolver](https://github.com/spray/sbt-revolver).


## Licence ##

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

Copyright &copy; 2014- Björn Westlin.

