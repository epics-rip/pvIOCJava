TODO
===========

xml
------------

The sax xml parser should be changed so that it no longer uses the pvData methods
that should be removed.
This includes: PVAuxInfo, PVField::replacePVField, PVStructure::appendPVField,
PVStructure::appendPVFields, PVStructure::removePVField,
PVStructure::getExtendsStructureName, and PVStructure::putExtendsStructureName.

This will also require changes in database and install.


pvIOCJava vs pvDatabaseJava
-------------

Should a project pvDatabaseJava be created that has functionality like pvDatabaseCPP?

simple example server
---------------
Make a simple example server that implements hello world just like pvDatabaseCPP
Add a section near beginning of pvIOCJava.html describing the example.
Als describe example/server/pvRecord.txt


pvIOCJava.html
---------------

The documention should be easier to understand.
