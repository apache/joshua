This provides a server demonstration for Joshua. You can load a model,
feed it sentences, add custom rules, and view the translations.

There are two steps:

1. Start Joshua in server mode

       $JOSHUA/bin/joshua -server-type http -server-port 5674 \
         -feature-function OOVPenalty \
         -feature-function "PhrasePenalty -owner custom" \
         -weight-overwrite "OOVPenalty 1 PhrasePenalty -1" \
         -mark-oovs -lowercase -projectcase -output-format %S

   Alternately, you can use the config file in this directory, which
   contains all the above parameteres, and simply run it like this:

       $JOSHUA/bin/joshua -config demo.config

   As a third option, you could pass it your own config file on a real
   pre-built model, such as Joshua's language packs.

   Command-line arguments override values in the config file, so if
   you need to change the port only, you can do:
   
       $JOSHUA/bin/joshua -config demo.config -server-port 5675
       
1. Load the index.html file, and make sure the values in the "Parameters"
   tab match your server settings above. You can also pass these values
   in the query string, e.g.,

       index.html?port=5674&server=localhost
   
That's it!
