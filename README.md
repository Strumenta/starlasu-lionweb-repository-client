# StarLasu LionWeb Repository Client

This is a Kotlin library to store and retrieve LionWeb trees and StarLasu ASTs into and from the [LionWeb Repository](https://github.com/LionWeb-io/lionweb-repository).

It consists of two clients:
* The LionWeb Client: it permits to store and retrieve LionWeb trees into and from the LionWeb Repository, according to the bulk API specified in LionWeb, plus supporting other extra calls exposed by the LionWeb Repository
* The StarLasu Client: it permits to store and retrieve Kolasu ASTs into and from the LionWeb Repository. It is essentially a wrapper around the LionWeb Client, as it convert Kolasu ASTs into and from LionWeb trees and then invoke the LionWeb Client for the actual communication with the LionWeb Repository.
