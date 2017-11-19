
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Todo: If there is a key entry in hashmap, its corresponding value cannot be empty
 */

public class TxHandler {

    private UTXOPool _utxoPool;
    private UTXOPool _claimedUtxoPool;
    private List<Transaction> _validTxs;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        _utxoPool = new UTXOPool(utxoPool);
        _claimedUtxoPool = new UTXOPool();
        _validTxs = new ArrayList<>();
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS

        _claimedUtxoPool = new UTXOPool();
        if(tx==null){
            return false;
        }

        List<Transaction.Input> inputList = tx.getInputs();
        List<Transaction.Output> outputList = tx.getOutputs();

        int i = 0;
        double inputSum = 0;
        double outputSum = 0;

        for(Transaction.Output output: outputList){
            if(output==null){
              continue;
            }
            if(Double.compare(output.value,(double)0)<0){
                  return false;
            }
            outputSum = outputSum + output.value;
        }

        for(Transaction.Input input: inputList){

          if(input==null){
              continue;
            }
            byte[] message = tx.getRawDataToSign(i);
            byte[] sign = input.signature;
            byte[] prevTxHash = input.prevTxHash;
            int prevTxOutputIndex = input.outputIndex;

            if(prevTxHash==null|| sign==null || message==null){
              continue;
            }

            UTXO utxo = new UTXO(prevTxHash, prevTxOutputIndex);

            if(utxo==null){
              continue;
            }

            if(!(_utxoPool.contains(utxo))){
                return false;
            }

            Transaction.Output prevTxOutput =  _utxoPool.getTxOutput(utxo);

            if (prevTxOutput==null){
              continue;
            }

            inputSum = inputSum + prevTxOutput.value;
            PublicKey publicKey = prevTxOutput.address;

            boolean validSign = Crypto.verifySignature(publicKey, message, sign); //publicKey.verifySignature(message, sign); //
            if(!validSign){
                return false;
            }
            i++;

            if(_claimedUtxoPool.contains(utxo)){
              return false;
            }

            _claimedUtxoPool.addUTXO(utxo, prevTxOutput);
        }

        if( Double.compare(outputSum,inputSum)>0){
            return false;
        }

      return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
      _validTxs = new ArrayList<>();
      for(Transaction transaction: possibleTxs){
        boolean validTx = isValidTx(transaction);

        if(validTx) {
          _validTxs.add(transaction);
          List<Transaction.Input> inputList = transaction.getInputs();

          for(Transaction.Input input: inputList){
            byte[] prevTxHash = input.prevTxHash;
            int prevTxOutputIndex = input.outputIndex;
            UTXO utxo = new UTXO(prevTxHash, prevTxOutputIndex);
            _utxoPool.removeUTXO(utxo);
          }

          int ind = 0;
          byte[] hash = transaction.getHash();
          List<Transaction.Output> outputList = transaction.getOutputs();
          for(Transaction.Output output: outputList){
            UTXO utxo = new UTXO(hash, ind);
            _utxoPool.addUTXO(utxo, output);
            ind++;
          }

        }
      }

      Transaction[] txArray = new Transaction[_validTxs.size()];
      txArray = _validTxs.toArray(txArray);
      return txArray;
  }
}
