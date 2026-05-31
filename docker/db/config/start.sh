#!/bin/bash

 (
sleep 30;
echo "Started sqlserver";

SQLCMD=/opt/mssql-tools18/bin/sqlcmd;
if [ ! -x $SQLCMD ]; then
    SQLCMD=/opt/mssql-tools/bin/sqlcmd
        if [ ! -x $SQLCMD ]; then
                echo "sqlcmd not available at $SQLCMD, unable to execute custom setup."
                exit 1
        fi
fi

echo "$SQLCMD -C -S localhost -U sa -P $SA_PASSWORD -i init.sql";
$SQLCMD -C -S localhost -U sa -P $SA_PASSWORD -i init.sql;
) & /opt/mssql/bin/sqlservr
