./spend.sh | grep "transaction" | cut -d":" -f2 | xargs ./tx.sh
