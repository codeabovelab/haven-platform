## Concept ##

System use Access Control List (ACL) extended to use roles as security model. Also it based on 
[spring ACL](http://docs.spring.io/spring-security/site/docs/current/reference/html/domain-acls.html).

## Authentication ##

System support BasicAuth and token-based authentication. Token is obtain by login-password pair. Token has 
lifetime, also each token usage update lifetime on configured timeout.

### Token ###

Token can be obtain through `/ui/token/login` entry point:

    curl -X POST --header 'Content-Type: application/json' \
    --data-raw '{"username":"admin","password":"password"}' \
    'http://cluman.server:8761/ui/token/login'

It return json object like:

    {
      "userName": "admin",
      "key": "sit:MAA..TL;DR..KO===",
      "creationTime": "2016-11-16T15:44:24.266",
      "expireAtTime": "2016-11-17T15:44:24.266"
    }

Response has 'key' with token data. So you must place it key in 'X-Auth-Token' header. 
Note that toke will expire at `expireAtTime` (`dm.token.expireAfterInSec` in config), but each request with token
will prolong token lifetime to `expireLastAccessInSec` value (`dm.token.expireAfterInSec` in config).   

For getting new token you can invoke `/ui/token/refresh`:

    curl -X PUT -H 'X-Auth-Token: sit:MAA..TL;DR..KO===' 'http://cluman.server:8761/ui/token/refresh'

It return same result as `/ui/token/login`.

## Authorization ##

Authorization based on following objects:

* Principal - simply user. 
* Granted Authority - usual it is role, user may has many roles. Some roles may be predefined, but system support adding custom roles.
* ObjectIdentifier (OID) - identifier of object, it object, but we can save it as string like `<TYPE>':s:'<id>`
* Security Identity (SID) - identifier of principal (user) or granted authority.  
* Access Control List (ACL) - contains oid, owner SID and list of ACEs.
* Access Control Entry (ACE) - entry which contain SID, permission and grant/revoke flag, it also has id - which is 
used only for manipulation by them.

