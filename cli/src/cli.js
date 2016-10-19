import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()
const chalk = cli.chalk

let username
let server

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> [host] [port]')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    args.host = (args.host === undefined ? 'localhost' : args.host)
    args.port = (args.part === undefined ? '8080' : args.port)
    server = connect({ host: args.host, port: args.port }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      let message = (Message.fromJSON(buffer))

      switch (message.command) {
        case 'connect':
          this.log(chalk.white.bgGreen(message.toString())); break
        case 'disconnect':
          this.log(chalk.white.bgRed(message.toString())); break
        case 'echo':
          this.log(chalk.blue(message.toString())); break
        case 'broadcast':
          this.log(chalk.cyan(message.toString())); break
        case 'users':
          this.log(chalk.yellow(message.toString())); break
        case 'whisper':
          this.log(chalk.magenta(message.toString())); break
      }
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    const [ command, ...rest ] = words(input, /[^, ]+/g)
    const contents = rest.join(' ')

    if (command === 'disconnect') {
      server.end(new Message({ username, command }).toJSON() + '\n')
    } else {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    }

    callback()
  })
