import javax.swing.plaf.synth.SynthLookAndFeel;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class Test
{
    private static final int numOfOutputsInPool = 5;

    public static void main(String[] argx) throws NoSuchAlgorithmException {
        // Create pool of privious transactions outputs
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = kpg.genKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        UTXOPool utxoPool = new UTXOPool();
        double txValue = 100;
        byte[] hash = {1, 2, 3, 4, 5};
        byte[] sig = {11, 22, 33, 44, 55};

        Transaction firstTx = new Transaction();
        firstTx.setHash(hash);

        for(int i = 0; i < numOfOutputsInPool; ++i)
        {
            firstTx.addOutput(txValue, publicKey);
        }

        int i = 0;
        ArrayList<Transaction.Output> outputs = firstTx.getOutputs();
        for(Transaction.Output out : outputs)
        {
            UTXO utxo = new UTXO(firstTx.getHash(), i++);
            utxoPool.addUTXO(utxo, out);
        }

        // Create new transaction
        // ~~~~~~~~~~~~~~~~~~~~~~
        Transaction secondTx = new Transaction();
        secondTx.addOutput(100, publicKey);
        secondTx.addOutput(300, publicKey);
        secondTx.addInput(firstTx.getHash(), 0);
        secondTx.addInput(firstTx.getHash(), 1);
        secondTx.addInput(firstTx.getHash(), 2);
        secondTx.addInput(firstTx.getHash(), 3);
        secondTx.addInput(firstTx.getHash(), 4);
        secondTx.setHash(hash);

        // Test the second transaction
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~
        boolean txValid;
        TxHandler txHandler = new TxHandler(utxoPool);
        txValid = txHandler.isValidTx(secondTx);

        if(txValid)
        {
            System.out.println("Transaction is valid");
        }
        else
        {
            System.out.println("Transaction is invalid");
        }
    }
}
