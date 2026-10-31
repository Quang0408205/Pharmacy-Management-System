#!/bin/sh
mongosh --quiet -u "$MONGO_INITDB_ROOT_USERNAME" -p "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin --eval '
try {
  if (rs.status().ok === 1) quit(0);
} catch (error) {
  const result = rs.initiate({ _id: "rs0", members: [{ _id: 0, host: "mongodb:27017" }] });
  quit(result.ok === 1 ? 0 : 1);
}
' || exit 1
mongosh --quiet -u "$MONGO_INITDB_ROOT_USERNAME" -p "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin --eval 'quit(db.hello().isWritablePrimary ? 0 : 1)'
