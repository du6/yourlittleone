Your Little One
=============================================

A web application for organizing activities.

## Products
- [App Engine][1]

## Language
- [Java][2]

## APIs
- [Google Cloud Endpoints][3]
- [Google App Engine Maven plugin][4]

## Developing Instructions

1. Run the application with `mvn appengine:devserver`, and ensure it's
   running by visiting your local server's address (by default
   [localhost:8080][5].)

1. Get the client library with

   $ mvn appengine:endpoints_get_client_lib

   It will generate a jar file named something like
   `yourlittleone-1.0-SNAPSHOT.jar` under the `target` directory of your
   project, as well as install the artifact into your local maven
   repository.

1. Deploy your application to Google App Engine with

   $ mvn appengine:update

[1]: https://developers.google.com/appengine
[2]: http://java.com/en/
[3]: https://developers.google.com/appengine/docs/java/endpoints/
[4]: https://developers.google.com/appengine/docs/java/tools/maven
[5]: https://localhost:8080/
