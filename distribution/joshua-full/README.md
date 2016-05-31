# Overview

Joshua is an open-source statistical machine translation decoder for phrase-based 
(new in 6.0), hierarchical, and syntax-based machine translation, written in Java. 
It is developed at the Human Language Technology Center of Excellence at Johns 
Hopkins University.

This charm provides the full development environment which allows users to both build 
and deploy language packs to the server and run translations against them.

To build language packs it is advised you use this charm in conjunction with one of the
Hadoop bundles available in the charm store to allow you to make use of a full Hadoop
cluster for the Thrax execution

# Usage

To deploy joshua-full:

   juju deploy cs:~apachesoftwarefoundation/joshua-full

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

