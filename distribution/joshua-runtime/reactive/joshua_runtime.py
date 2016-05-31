#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

from charms.reactive import when, when_not, set_state
from charmhelpers.fetch.archiveurl import ArchiveUrlFetchHandler
from charmhelpers.core import hookenv
from subprocess import check_call, CalledProcessError, call, check_output, Popen
from charmhelpers.core.hookenv import status_set, log
from charms.reactive.helpers import data_changed
import subprocess
import os

au = ArchiveUrlFetchHandler()
os.environ["JOSHUA"] = "/opt/joshua-runtime-6.0.5/"
port =  hookenv.config('port')

@when_not('joshua-runtime.installed')
def install_joshua_runtime():
    status_set('maintenance', 'Joshua')
    download()
    unzip()
    set_state('joshua-runtime.installed')

def download():
    au.download("http://community.meteorite.bi/joshua-runtime.tgz", "/tmp/joshua-runtime.tgz")

def unzip():
    check_output(['tar', 'xvfz', "/tmp/joshua-runtime.tgz", '-C', '/opt'])

@when_not('java.ready')
def update_java_status():
    status_set('blocked', 'Waiting for Java.')

@when_not('languagepack.installed')
def update_lp_status():
    status_set('blocked', 'Waiting for Language Pack')

@when('java.ready')
@when('joshua-runtime.installed')
@when('languagepack.installed')
def start_joshua(java):
    if (data_changed('port', hookenv.config())):
        port = hookenv.config('port')
        call(['pkill', '-f', 'joshua.decoder.JoshuaDecoder'])
    try:
        check_call(['pgrep', '-f', 'joshua.decoder.JoshuaDecoder'])
    except CalledProcessError:
        log("Starting:"+ hookenv.config('port'))
        Popen(['nohup /opt/joshua-runtime-6.0.5/bin/joshua-decoder -m 4g -c joshua.config -server-port '+hookenv.config('port')+' $* &'], cwd='/opt/language-pack', env=dict(os.environ, JOSHUA="/opt/joshua-runtime-6.0.5/"), stdout=subprocess.PIPE,stderr=subprocess.PIPE,shell=True)
        hookenv.open_port(hookenv.config('port'))
    status_set('active', 'Joshua running')

@when('joshua-runtime.install')
@when_not('languagepack.installed')
def stop_joshua():
    call(['pkill', '-f', 'joshua.decoder.JoshuaDecoder'])
    status_set('blocked', 'Waiting for language pack')
