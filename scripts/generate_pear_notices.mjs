import fs from 'node:fs'
import path from 'node:path'

const args = process.argv.slice(2)
const valueAfter = (flag) => {
  const index = args.indexOf(flag)
  return index === -1 ? null : args[index + 1]
}
const root = path.resolve(valueAfter('--root') || process.cwd())
const output = path.resolve(valueAfter('--output'))
const bareKitDir = path.resolve(valueAfter('--bare-kit-dir'))
const lock = JSON.parse(fs.readFileSync(path.join(root, 'package-lock.json'), 'utf8'))
const readFirst = (directory, names) => {
  for (const name of names) {
    const file = path.join(directory, name)
    if (fs.existsSync(file)) return fs.readFileSync(file, 'utf8').trim()
  }
  return null
}
const sourceUrl = (metadata) => {
  const repository = typeof metadata.repository === 'string'
    ? metadata.repository
    : metadata.repository?.url
  return repository?.replace(/^git\+/, '').replace(/\.git$/, '') || metadata.homepage || null
}
const records = Object.entries(lock.packages || {})
  .filter(([packagePath, metadata]) => packagePath.startsWith('node_modules/') && !metadata.dev)
  .map(([packagePath, metadata]) => {
    const directory = path.join(root, packagePath)
    return {
      name: metadata.name || packagePath.replace(/^node_modules\//, ''),
      version: metadata.version || 'unknown',
      license: metadata.license || 'UNKNOWN',
      source: sourceUrl(metadata),
      licenseText: readFirst(directory, ['LICENSE', 'LICENSE.md', 'LICENSE.txt', 'LICENCE', 'COPYING']),
      noticeText: readFirst(directory, ['NOTICE', 'NOTICE.md', 'NOTICE.txt'])
    }
  })
records.push({
  name: 'Bare Kit',
  version: '2.3.0',
  license: 'Apache-2.0',
  source: 'https://github.com/holepunchto/bare-kit',
  licenseText: readFirst(bareKitDir, ['LICENSE']),
  noticeText: readFirst(bareKitDir, ['NOTICE'])
})
records.sort((a, b) => a.name.localeCompare(b.name) || a.version.localeCompare(b.version))
const counts = records.reduce((result, record) => {
  result[record.license] = (result[record.license] || 0) + 1
  return result
}, {})
const lines = [
  '# Third-party notices for Ncy Pear transport',
  '',
  'Generated from the locked Android production dependency tree. Do not edit manually.',
  '',
  'License inventory:',
  ...Object.entries(counts).sort().map(([license, count]) => `- ${license}: ${count}`),
  ''
]
for (const record of records) {
  lines.push(`## ${record.name} ${record.version} — ${record.license}`, '')
  if (record.source) lines.push(`Source: ${record.source}`, '')
  if (record.noticeText) lines.push('Notice:', '', '```text', record.noticeText, '```', '')
  if (record.licenseText) {
    lines.push('License:', '', '```text', record.licenseText, '```', '')
  } else {
    lines.push(
      `The published package declares SPDX license ${record.license}, but does not include a license text file.`,
      'Consult the source link above for its licensing metadata.',
      ''
    )
  }
}
fs.mkdirSync(path.dirname(output), { recursive: true })
fs.writeFileSync(output, `${lines.join('\n')}\n`, 'utf8')
console.log(`Wrote ${records.length} dependency notices to ${output}`)
