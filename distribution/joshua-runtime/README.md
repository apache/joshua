# Overview

Joshua is an open-source statistical machine translation decoder for phrase-based 
(new in 6.0), hierarchical, and syntax-based machine translation, written in Java. 
It is developed at the Human Language Technology Center of Excellence at Johns 
Hopkins University.

This charm provides the runtime environment which allows users to deploy language
packs to the server and run translations against them.

There are a number of language packs available and developers are able to build
their own using the joshua-full charm available in the charm store.


# Usage

To deploy joshua-runtime:

   juju deploy cs:~apachesoftwarefoundation/joshua-runtime

## Known Limitations and Issues

Currently Joshua only supports a single language pack deployed against it at once.

# Configuration

Port: specify the port you want the Joshua http interface to run on for remote
calls to the Joshua server.

Memory: amount of RAM the server should consume. 

# Contact Information

To contact the authors swing by the dev mailing list:
dev@joshua.incubator.apache.org

## Apache Joshua

  - http://joshua.incubator.apache.org
  - https://issues.apache.org/jira/browse/joshua
  - dev@joshua.incubator.apache.org

