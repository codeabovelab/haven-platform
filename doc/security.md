## Concept ##

System use Access Control List (ACL) extended to use roles as security model. Also it based on 
[spring ACL](http://docs.spring.io/spring-security/site/docs/current/reference/html/domain-acls.html).

## Authentication ##

System support BasicAuth and token-based authentication. Token is obtain by login-password pair. Token has 
lifetime, also each token usage update lifetime on configured timeout.

## Authorization ##

Authorization based on following objects:

* Principal - simply user. 
* Granted Authority - usual it is role, user may has many roles. Some roles may be predefined, but system support adding custom roles.
* ObjectIdentifier (OID) - identifier of object, it object, but we can save it as string like `<TYPE>':s:'<id>`
* Security Identity (SID) - identifier of principal (user) or granted authority.  
* Access Control List (ACL) - contains oid, owner SID and list of ACEs.
* Access Control Entry (ACE) - entry which contain SID, permission and grant/revoke flag, it also has id - which is 
used only for manipulation by them.

