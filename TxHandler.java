import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.TransferQueue;

public class TxHandler {


    private UTXOPool pool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool)
    {
        pool = new UTXOPool(utxoPool);
    }

    public void AddTransactionOutputsToPool(Transaction tx)
    {
        ArrayList<Transaction.Output> outputs = tx.getOutputs();

        int i = 0;
        byte[] hash = tx.getHash();

        for(Transaction.Output output : outputs)
        {
            pool.addUTXO(new UTXO(hash, i++), output);
        }
    }

    public void RemoveTransactionInputsFromPool(Transaction tx)
    {
        ArrayList<Transaction.Input> txInputs = tx.getInputs();

        for(Transaction.Input in : txInputs)
        {
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);

            pool.removeUTXO(utxo);
        }
    }

    private boolean CheckThatAllOutputsInCurrUTXOPool(Transaction tx)
    {
        boolean result = true;

        ArrayList<Transaction.Input> txInputs = tx.getInputs();
        //byte[] hash = tx.getHash();

        for(Transaction.Input in : txInputs)
        {
            if(!pool.contains(new UTXO(in.prevTxHash, in.outputIndex)))
            {
                result = false;
                break;
            }
        }

        return result;
    }

    private boolean CheckThatAllSignaturesAreValid(Transaction tx)
    {
//        if(true) //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//            return true;

        boolean result = true;

        ArrayList<Transaction.Input> txInputs = tx.getInputs();
        //byte[] hash = tx.getHash();
        //int i = 0;

        for(Transaction.Input in : txInputs)
        {
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
            Transaction.Output out = pool.getTxOutput(utxo);
            byte[] message = tx.getRawDataToSign(txInputs.indexOf(in));
            if(!Crypto.verifySignature(out.address, message, in.signature))
            {
                result = false;
            }
        }

        return result;
    }

    private boolean CheckThatNoUTXOClaimedTwice(Transaction tx)
    {
        boolean result = true;

        ArrayList<Transaction.Input> txInputs = tx.getInputs();
        ArrayList<UTXO> txClaimedUTXOs = new ArrayList<>();
        byte[] hash = tx.getHash();

        // Get list of all UTXOs claimed by transaction
        for(Transaction.Input in : txInputs)
        {
            txClaimedUTXOs.add(new UTXO(hash, in.outputIndex));
        }

        //Check for duplicates
        UTXO[] UTXOs = new UTXO[txClaimedUTXOs.size()];
        UTXOs = txClaimedUTXOs.toArray(UTXOs);

        for (int i = 0; i < UTXOs.length; i++)
        {
            // Find the duplicate
            for (int j = 0; j < UTXOs.length; j++)
            {
                if (UTXOs[i].equals(UTXOs[j]) && i != j)
                {
                    result = false;
                }
            }
        }

        return result;
    }

    private boolean CheckThatAllOutputsAreNonNegative(Transaction tx)
    {
        boolean result = true;

        ArrayList<Transaction.Output> txOutputs = tx.getOutputs();

        for(Transaction.Output out : txOutputs)
        {
            if(out.value < 0)
            {
                result = false;
            }
        }

        return result;
    }

    private boolean CheckThatInputValuesAreGreaterThanOutput(Transaction tx)
    {
        boolean result;

        double inputSum = 0;
        double outputSum = 0;


        // Get input sum
        // ~~~~~~~~~~~~~
        ArrayList<Transaction.Input> txInputs = tx.getInputs();
        //byte[] hash = tx.getHash();

        for(Transaction.Input in : txInputs)
        {
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
            Transaction.Output out = pool.getTxOutput(utxo);
            inputSum += out.value;
        }

        // Get output sum
        // ~~~~~~~~~~~~~~
        ArrayList<Transaction.Output> txOutputs = tx.getOutputs();

        for(Transaction.Output out : txOutputs)
        {
            outputSum += out.value;
        }

        result = (inputSum >= outputSum);

        return result;
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx)
    {
        return  CheckThatAllOutputsInCurrUTXOPool(tx) &&
                CheckThatAllSignaturesAreValid(tx) &&
                CheckThatNoUTXOClaimedTwice(tx) &&
                CheckThatAllOutputsAreNonNegative(tx) &&
                CheckThatInputValuesAreGreaterThanOutput(tx);

//        boolean isCheckThatAllOutputsInCurrUTXOPool = CheckThatAllOutputsInCurrUTXOPool(tx);
//        boolean isCheckThatAllSignaturesAreValid = CheckThatAllSignaturesAreValid(tx);
//        boolean isCheckThatNoUTXOClaimedTwice = CheckThatNoUTXOClaimedTwice(tx);
//        boolean isCheckThatAllOutputsAreNonNegative = CheckThatAllOutputsAreNonNegative(tx);
//        boolean isCheckThatInputValuesAreGreaterThanOutput = CheckThatInputValuesAreGreaterThanOutput(tx);
//
//
//       return true;
    }


    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs)
    {
        ArrayList<Transaction> txs = new ArrayList<>(Arrays.asList(possibleTxs));
        ArrayList<Transaction> validTxs = new ArrayList<>();
        boolean validTxFound = true;

        while(!txs.isEmpty() && validTxFound)
        {
            validTxFound = false;

            //for (Transaction tx : txs)
            for (Iterator<Transaction> txItr = txs.iterator(); txItr.hasNext(); )
            {
                Transaction tx = txItr.next();
                if (isValidTx(tx))
                {
                    AddTransactionOutputsToPool(tx);
                    RemoveTransactionInputsFromPool(tx);
                    validTxs.add(tx);
                    txItr.remove();
                    validTxFound = true;
                    break;
                }
            }
        }

        Transaction[] result = validTxs.toArray(new Transaction[validTxs.size()]);

        return result;
    }
}
