#!/usr/bin/env node
// CloudSystem Node Installer - Cross-platform (Node.js)

const os = require('os');
const { execSync } = require('child_process');
const fs = require('fs');
const https = require('https');
const http = require('http');

const platform = os.platform();
const SERVER_URL = process.env.SERVER_URL || 'http://localhost:5000';

function runCommand(cmd) {
    try {
        execSync(cmd, { stdio: 'inherit' });
        return true;
    } catch (e) {
        return false;
    }
}

function checkJava() {
    try {
        execSync('java -version', { stdio: 'ignore' });
        return true;
    } catch (e) {
        return false;
    }
}

function installJava() {
    console.log('A instalar Java...');
    if (platform === 'win32') {
        return runCommand('winget install Eclipse.Adoptium.JDK17');
    } else if (platform === 'linux') {
        if (runCommand('which apt-get')) {
            return runCommand('apt-get update && apt-get install -y default-jre');
        } else if (runCommand('which yum')) {
            return runCommand('yum install -y java-17-openjdk');
        }
    } else if (platform === 'darwin') {
        return runCommand('brew install openjdk@17');
    }
    console.log('Erro: Não foi possível instalar Java automaticamente.');
    return false;
}

function downloadJar() {
    console.log('A baixar o nó...');
    const url = new URL(`${SERVER_URL}/nodes/jar`);
    const protocol = url.protocol === 'https:' ? https : http;
    const file = fs.createWriteStream('cloudsystem-node.jar');
    return new Promise((resolve, reject) => {
        protocol.get(url, (response) => {
            response.pipe(file);
            file.on('finish', () => { file.close(); resolve(true); });
        }).on('error', (err) => { fs.unlink('cloudsystem-node.jar', () => {}); reject(err); });
    });
}

function configNode() {
    const readline = require('readline').createInterface({
        input: process.stdin,
        output: process.stdout
    });
    const ask = (q, d) => new Promise(r => readline.question(q, ans => r(ans || d)));

    (async () => {
        const serverIp = await ask('IP do servidor [localhost]: ', 'localhost');
        const nodePort = await ask('Porta do nó [8081]: ', '8081');
        const quotaGb = await ask('Quota em GB [100]: ', '100');

        const properties = `cloud.node.server-url=http://${serverIp}:5000\ncloud.node.server-port=${nodePort}\ncloud.node.quota-bytes=${quotaGb * 1024 * 1024 * 1024}\ncloud.node.auto-register=true\n`;
        fs.writeFileSync('node.properties', properties);

        readline.close();
        console.log('A iniciar o nó...');
        runCommand(`java -jar cloudsystem-node.jar --spring.config.location=node.properties`);
    })();
}

async function main() {
    console.log('=== CloudSystem Node Installer ===');

    if (!checkJava()) {
        if (!installJava()) {
            process.exit(1);
        }
    }

    try {
        await downloadJar();
    } catch (e) {
        console.log('Erro: Falhou o download do .jar.');
        process.exit(1);
    }

    configNode();
}

main();
