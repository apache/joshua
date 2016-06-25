#!/usr/bin/python

"""
Starts a web server on port 8000. Start it in the current directory and
it will serve up index.html.
"""

import sys
import SimpleHTTPServer
import SocketServer

port = 8000

handler = MyRequestHandler
httpd = SocketServer.TCPServer(("", port), handler)
httpd.serve_forever()
