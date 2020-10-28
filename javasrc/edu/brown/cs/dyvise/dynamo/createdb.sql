#! /bin/csh -f

set DB = dyview_${USER}_$1



psql -h bridget clime <<EOF
DROP DATABASE $DB
EOF

psql -h bridget clime <<EOF
CREATE DATABASE $DB
EOF

psql -h bridget $DB <<EOF

CREATE TABLE SrcClass (
   name text NOT NULL,
   type text,
   superclass text,
   signature text,
   source text,
   access int,
   abstract boolean,
   enum boolean,
   final boolean,
   iface boolean,
   private boolean,
   protected boolean,
   public boolean,
   static boolean,
   project boolean
);

CREATE TABLE SrcInterface (
   type text NOT NULL,
   iface text NOT NULL,
   super boolean
);

CREATE TABLE SrcField (
   id text NOT NULL,
   name text NOT NULL,
   class text NOT NULL,
   type text,
   signature text,
   access int,
   final boolean,
   private boolean,
   protected boolean,
   public boolean,
   static boolean,
   transient boolean,
   volatile boolean
);


CREATE TABLE SrcMethod (
   id text NOT NULL,
   name text NOT NULL,
   class text NOT NULL,
   type text,
   signature text,
   numarg int,
   returns text,
   access int,
   abstract boolean,
   final boolean,
   native boolean,
   private boolean,
   protected boolean,
   public boolean,
   static boolean,
   synchronized boolean,
   varargs boolean
);


CREATE TABLE SrcMethodParam (
   methodid text NOT NULL,
   name text,
   type text,
   indexno int
);


CREATE TABLE SrcCall (
   methodid text,
   toclass text,
   tomethod text,
   totype text
);


CREATE TABLE SrcLines (
   methodid text,
   lineno int,
   startoffset int
);


CREATE TABLE SrcAlloc (
   methodid text,
   class text
);


CREATE TABLE CompClassHierarchy (
   supertype text,
   subtype text,
   super boolean
);


CREATE TABLE CompEventAccess (
   class text,
   method text,
   access text
);

CREATE TABLE UpdateTimes (
   what text,
   main text,
   lastupdated timestamp
);

CREATE TABLE NameMaps (
   main text,
   id text
);


EOF

echo DATABASE $DB setup

