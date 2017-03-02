This Docker container installs KenLM and uses it to start a language pack with KenLM
language models instead of BerkeleyLM ones. It requires version 3 or above language packs.

To use it, you need to do two things when running docker:

- Mount the version 3 language pack to /model
- Choose a local (host) port and bind it to the docker port that Joshua will run on

This can be accomplished with the following command:

    docker run -p 127.0.0.1:5674:5674 -v /path/to/LP:/model -it joshua/kenlm

This will make the language pack available on port 5674 on localhost.
