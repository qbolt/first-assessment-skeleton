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
      const message = (Message.fromJSON(buffer))
      const timestamp = chalk.white(message.timestamp)
      switch (message.command) {
        case 'connect':
          this.log(timestamp + chalk.green(message.toString())); break
        case 'disconnect':
          this.log(timestamp + chalk.red(message.toString())); break
        case 'success':
          this.log(chalk.green(message.toString())); break
        case 'echo':
          this.log(timestamp + chalk.blue(message.toString())); break
        case 'broadcast':
          this.log(timestamp + chalk.cyan(message.toString())); break
        case 'users':
          this.log(timestamp + chalk.yellow(message.toString())); break
        case 'alert':
          this.log(chalk.red(message.toString())); break
        case 'help':
          this.log(chalk.white(message.toString())); break
        default:
          if (message.command.substring(0, 1) === '@') {
            this.log(timestamp + chalk.magenta(message.toString())); break
          } else {
            this.log(timestamp + chalk.white(message.toString())); break
          }
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
      const timestamp = new Date().getTime()
      server.write(new Message({ username, command, contents, timestamp }).toJSON() + '\n')
    }

    callback()
  })
