# Social App - Cesar Purper

Requisites:

A user is able to enter the social media app with its name and email, create a Post and view other users Posts.

A post can have text and image, which can be uploaded by the user.


Minimum Requirements:

- User registration with name and email
  - Shouldn't allow duplicate emails
- Endpoint for retrieving posts with the content, image, date and author
  - Sorting by date should be available
  - Filtering by user should be available
- Endpoint for creating posts with text and image
- Endpoint for editing own post
  - Ability to change image

Nice to Have:

- Image compression before saving the uploaded image
- Capability of following another user's Posts, receiving notifications every time a new one is created

Tech requirements:

Language: Scala
Frameworks/Libraries: Http4s, Akka or similar
Relational database: Postgres or MySQL
Tests: Unit and IT
Architecture/Design Principles: README.md should explain architecture decisions
Dockerized: Should be able to run in a Docker container via compose
VCS: A version control system needs to be used  

What will be evaluated:

Language domain
Framework/Library fit and usage
Data, domain and API modelling
Architecture for scalability
Testable code using TDD
Test coverage
Readability
Design principles
Design patterns
DDD (Domain Driven Design)
Code commits, comments, history, etc
Readme clarity on how it was solved, what is missing and why, and how to run it
