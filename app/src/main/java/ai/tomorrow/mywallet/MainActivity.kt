package ai.tomorrow.mywallet

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.consenlabs.tokencore.wallet.Identity
import org.consenlabs.tokencore.wallet.KeystoreStorage
import org.consenlabs.tokencore.wallet.Wallet
import org.consenlabs.tokencore.wallet.WalletManager
import org.consenlabs.tokencore.wallet.model.ChainType
import org.consenlabs.tokencore.wallet.model.Metadata
import org.consenlabs.tokencore.wallet.model.Network
import org.consenlabs.tokencore.wallet.transaction.EthereumTransaction
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.utils.Numeric
import java.nio.file.Path
import java.nio.file.Paths


class MainActivity : AppCompatActivity(), KeystoreStorage {


    private val TAG = "MainActivity"
    private val SAMPLE_MNEMONIC1 = "world tired copper write maid monkey risk today husband hope grid inflict"
    private val SAMPLE_MNEMONIC2 = "mouse inject office junior repeat one tip actor drift love auto chase"

    private val web3j = Web3j.build(HttpService("https://ropsten.infura.io/llyrtzQ3YhkdESt2Fzrk"))

    private var uiHandler = Handler()
    private lateinit var backgroundHandler: Handler
    private lateinit var backgroundThread: HandlerThread

    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main)

    private lateinit var identity1: Identity
    private lateinit var identity2: Identity

    private lateinit var wallet1: Wallet
    private lateinit var wallet2: Wallet


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WalletManager.storage = this
        WalletManager.scanWallets()

        setupWidgets()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.update_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.updateMenu){
            Log.d(TAG, "update the balances")
            Toast.makeText(this, "update the balances.", Toast.LENGTH_SHORT).show()
            val balance1 = getBalance(wallet1.address)
            balanceTextView1.text = balance1.toString()
            val balance2 = getBalance(wallet2.address)
            balanceTextView2.text = balance2.toString()
        }
        return true
    }

    private fun setupWidgets() {
        sampleMnemonicBtn1.setOnClickListener { mnemonicEditText1.setText(SAMPLE_MNEMONIC1) }
        sampleMnemonicBtn2.setOnClickListener { mnemonicEditText2.setText(SAMPLE_MNEMONIC2) }

        getWalletBtn1.setOnClickListener {
            if (mnemonicEditText1.text.equals("")) {
                Toast.makeText(this, "you can't use an empty mnemonic.", Toast.LENGTH_SHORT).show()
            } else {
                identity1 = Identity.recoverIdentity(
                    mnemonicEditText1.text.toString(),
                    "identity1",
                    "",
                    "",
                    Network.ROPSTEN,
                    Metadata.NONE
                )
                Log.d(TAG, "get identity1")
                wallet1 = identity1.wallets[0]
                val balance = getBalance(wallet1.address)
                balanceTextView1.text = balance.toString()
            }
        }

        getWalletBtn2.setOnClickListener {
            if (mnemonicEditText2.text.equals("")) {
                Toast.makeText(this, "you can't use an empty mnemonic.", Toast.LENGTH_SHORT).show()
            } else {
                identity2 = Identity.recoverIdentity(
                    mnemonicEditText2.text.toString(),
                    "identity2",
                    "",
                    "",
                    Network.ROPSTEN,
                    Metadata.NONE
                )
                Log.d(TAG, "get identity2")
                wallet2 = identity2.wallets[0]
                val balance = getBalance(wallet2.address)
                balanceTextView2.text = balance.toString()
            }
        }

        sendToAccount2Btn.setOnClickListener {
            Log.d(TAG, "send 1 ETH to account 2")
            send(wallet1, wallet2.address)
        }

        sendToAccount1Btn.setOnClickListener {
            Log.d(TAG, "send 1 ETH to account 1")
            send(wallet2, wallet1.address)
        }
    }

    private fun send(fromWallet: Wallet, toAddress: String){
        uiScope.launch {

            withContext(Dispatchers.IO){
                val ethGetTransactionCount = web3j.ethGetTransactionCount(
                    "0x${fromWallet.address}", DefaultBlockParameterName.LATEST
                ).send()
                val nonce = ethGetTransactionCount.transactionCount
                val gasLimit = BigInteger.valueOf(21000L)
//                val amount = BigInteger.valueOf(1)
                val amount = Convert.toWei(BigDecimal(1L), Convert.Unit.ETHER).toBigInteger()
                val gasPrice = web3j.ethGasPrice().send().gasPrice
                Log.d(TAG, "gasPrice: $gasPrice")
                Log.d(TAG, "nonce: $nonce")

                val keystorePath = filesDir.absolutePath + "/wallets" + "/${fromWallet.id}.json"
                val credentials = WalletUtils.loadCredentials("", keystorePath)

                val rawTransaction = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, toAddress, amount)
                val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)

                val hexValue = Numeric.toHexString(signedMessage)
                val ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send()

                Log.d(TAG, ethSendTransaction.toString())
                Log.d(TAG, "transactionHash: ${ethSendTransaction.transactionHash}")
                uiHandler.post{
                    if(ethSendTransaction.transactionHash != null){
                        Toast.makeText(this@MainActivity, "Transaction successfully!", Toast.LENGTH_SHORT).show()
                    } else{
                        Toast.makeText(this@MainActivity, "Transaction failed!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    private fun getBalance(address: String): BigDecimal{
        // connect to node
//        val web3 = Web3j.build(HttpService("https://ropsten.infura.io/llyrtzQ3YhkdESt2Fzrk"))  // defaults to http://localhost:8545/

        // send asynchronous requests to get balance
        val ethGetBalance = web3j
            .ethGetBalance("0x$address", DefaultBlockParameterName.LATEST)
            .sendAsync()
            .get()

        val wei = ethGetBalance.balance
        return Convert.fromWei(BigDecimal(wei), Convert.Unit.ETHER)
    }


    override fun getKeystoreDir(): File {
//        Log.d(TAG, "getKeystoreDir = $filesDir")
        return this.filesDir
    }
}
