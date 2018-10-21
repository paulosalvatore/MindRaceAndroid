package br.com.paulosalvatore.mindrace

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.concurrent.schedule

enum class Status
{
	ESPERANDO,
	INICIANDO,
	CORRENDO,
	FINALIZANDO,
	MENU;
}

class MainActivity : AppCompatActivity() {
	val ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION"

	var usbManager: UsbManager? = null
	var device: UsbDevice? = null
	var connection: UsbDeviceConnection? = null
	var serialPort: UsbSerialDevice? = null

	var concentracao1 = 0
	var concentracao2 = 0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		usbManager = getSystemService(USB_SERVICE) as UsbManager;

		val filter = IntentFilter()
		filter.addAction(ACTION_USB_PERMISSION)
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
		registerReceiver(broadcastReceiver, filter)

		btLimpar.setOnClickListener {
			tvArduino.text = ""
		}

		btIniciar.setOnClickListener {
			onClickStart()
		}

		btVoltar.setOnClickListener {
			logLayout.visibility = View.INVISIBLE
		}

		logo.setOnClickListener {
			logLayout.visibility = View.VISIBLE
		}

		sinal1.setOnClickListener {
			Timer("TesteConcentração", false).schedule(200) {
				val random1 = Random().nextInt(101)
				val random2 = Random().nextInt(101)
				runOnUiThread {
//					processarValor("1;1;$random1;1;1;$random2;1;2;20;1;1!")
					processarValor("0;0;0;1;0;0;0;0;5;1;0!\n")
				}
			}
		}

		trofeu1.visibility = View.INVISIBLE
		trofeu2.visibility = View.INVISIBLE
		carro.visibility = View.INVISIBLE
	}

	private val broadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			tvArduino.append("Intent Action ${intent.action}\n")

			if (intent.action == ACTION_USB_PERMISSION) {
				val granted = intent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)

				tvArduino.append("Granted $granted\n")

				if (granted) {
					connection = usbManager?.openDevice(device)
					serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection)
					serialPort?.let {

						tvArduino.append("Port Open ${it.open()}\n")

						if (it.open()) {
							it.setBaudRate(9600)
							it.setDataBits(UsbSerialInterface.DATA_BITS_8)
							it.setStopBits(UsbSerialInterface.STOP_BITS_1)
							it.setParity(UsbSerialInterface.PARITY_NONE)
							it.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
							it.read(mCallback)
							Toast.makeText(this@MainActivity, "Serial Connection Opened!", Toast.LENGTH_LONG).show()
						}
					}
				}
			} else if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
				onClickStart()
			} else if (intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
				serialPort?.close();
				tvArduino.append("Serial Connection Closed!\n")
				Toast.makeText(this@MainActivity, "Serial Connection Closed!", Toast.LENGTH_LONG).show()
			}
		}
	}

	private val mCallback = UsbSerialInterface.UsbReadCallback {
		runOnUiThread {
			val valorRecebido = String(it, charset("UTF-8"))

			processarValor(valorRecebido)
		}
	}

	private fun processarValor(valorRecebido: String) {
		val valorSeparado = valorRecebido.trim().split(";")

		try {
			val pareado1 = valorSeparado[0] == "1"
			val cabeca1 = valorSeparado[1] == "1"

			sinal1.post {
				if (pareado1) {
					sinal1.setImageResource(R.drawable.sinal_conectado)
				} else if (cabeca1) {
					sinal1.setImageResource(R.drawable.sinal_conectando3)
				} else {
					sinal1.setImageResource(R.drawable.sinal_desconectado)
				}
			}

			concentracao1 = lerp(concentracao1, valorSeparado[2].toInt(), 1f)

			if (concentracao1 == 99) {
				concentracao1 = 100
			}

			tvConcentracao_1.text = "$concentracao1%"
			arc_progress_1.progress = concentracao1

			val pareado2 = valorSeparado[3] == "1"
			val cabeca2 = valorSeparado[4] == "1"

			sinal2.post {
				if (pareado2) {
					sinal2.setImageResource(R.drawable.sinal_conectado)
				} else if (cabeca2) {
					sinal2.setImageResource(R.drawable.sinal_conectando3)
				} else {
					sinal2.setImageResource(R.drawable.sinal_desconectado)
				}
			}

			concentracao2 = lerp(concentracao2, valorSeparado[5].toInt(), 1f)

			if (concentracao2 == 99) {
				concentracao2 = 100
			}

			tvConcentracao_2.text = "$concentracao2%"
			arc_progress_2.progress = concentracao2

			val voltasAtual1 = valorSeparado[6].toInt()
			val voltasAtual2 = valorSeparado[7].toInt()
			tvVoltaAtual_1.text = String.format("%02d", voltasAtual1)
			tvVoltaAtual_2.text = String.format("%02d", voltasAtual2)

			val voltasMax = valorSeparado[8].toInt()
			tvVoltasMax_1.text = String.format("%02d", voltasMax)
			tvVoltasMax_2.text = String.format("%02d", voltasMax)

			val status = valorSeparado[9].toInt()

			// status == Status.ESPERANDO.ordinal

			val vencedor = valorSeparado[10].replace("!", "").toInt()

			val imagemCarro = when (vencedor) {
				1 -> R.drawable.carro1
				2 -> R.drawable.carro2
				else -> R.drawable.carro
			}

			carro.visibility = View.VISIBLE
			carro.post {
				carro.setImageResource(imagemCarro)
			}

			if (vencedor == 1) {
				trofeu1.post {
					trofeu1.setImageResource(R.drawable.trofeu_1)
					trofeu1.visibility = View.VISIBLE
				}

				trofeu2.post {
					trofeu2.setImageResource(R.drawable.trofeu_2)
					trofeu2.visibility = View.VISIBLE
				}
			} else if (vencedor == 2) {
				trofeu1.post {
					trofeu1.setImageResource(R.drawable.trofeu_2)
					trofeu1.visibility = View.VISIBLE
				}

				trofeu2.post {
					trofeu2.setImageResource(R.drawable.trofeu_1)
					trofeu2.visibility = View.VISIBLE
				}
			} else {
				trofeu1.visibility = View.INVISIBLE
				trofeu2.visibility = View.INVISIBLE
			}
		} catch (e: Exception) {
			tvArduino.append("$e")
		}

		tvArduino.append("callback $valorRecebido")
	}

	fun onClickStart() {
		tvArduino.append("onClickStart\n")

		val usbDevices = usbManager?.getDeviceList()

		tvArduino.append("UsbDevicesSize ${usbDevices?.size}\n")

		usbDevices?.let {
			if (it.isNotEmpty()) {
				var keep = true
				tvArduino.append("Entries ${usbDevices.entries}\n")
				for (entry in usbDevices.entries) {
					tvArduino.append("Entry $entry\n")
					device = entry.value
					val deviceVID = device?.getVendorId()

					tvArduino.append("deviceVID $deviceVID\n")

					if (deviceVID == 6790)
					{
						val pi = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
						usbManager?.requestPermission(device, pi)
						keep = false
					} else {
						tvArduino.append("Device ID not mapped\n")
						connection = null
						device = null
					}

					if (!keep)
						break
				}
			}
		}
	}

	fun lerp(a: Int, b: Int, f: Float): Int {
		return (a + f * (b - a)).toInt()
	}
}
