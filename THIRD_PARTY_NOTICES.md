# Third-party notices and license scope

The repository's original SHM code and documentation are licensed by their author, `hang4309`, under the root [MIT License](LICENSE), effective with this licensing commit. This covers the original Java application, Python collector and demo tools, Vue application, tests and project documentation. Third-party material retains its own terms; the root license does not relicense it.

## Bundled Maven Wrapper

`shm-backend-fable5-copy/mvnw` and `mvnw.cmd` identify themselves as Apache Maven Wrapper 3.3.4 and retain their existing Apache license headers without modification. A copy of the upstream [Apache-2.0 license](licenses/Apache-2.0.txt) and [Maven Wrapper NOTICE](licenses/maven-wrapper-NOTICE.txt) is included. Source: [Apache Maven Wrapper 3.3.4](https://github.com/apache/maven-wrapper/tree/maven-wrapper-3.3.4). Its downloaded Maven distribution retains its own licenses and notices.

## Dependencies installed separately

Dependency names, versions and applicable license metadata remain in their existing manifests and distributed packages. Their code is not relicensed by the SHM MIT grant.

| Area | Declared dependencies | Version / notice source |
| --- | --- | --- |
| Java backend | Spring Boot, MyBatis, MySQL Connector/J, Lombok, Spring test components, H2 | [pom.xml](shm-backend-fable5-copy/pom.xml), resolved dependency POMs and JAR `META-INF` licenses/notices |
| Vue application | Vue, Axios, SheetJS `xlsx` | [package.json](shm-frontend-fable5-copy/package.json), [package-lock.json](shm-frontend-fable5-copy/package-lock.json) and each installed package's license |
| Frontend build and lint | Vite, Vue Vite plugin, ESLint and plugins, Oxlint, globals, npm-run-all2 | The same frontend manifests and distributed package notices |
| Security CI | SHM API Guard; GitHub Actions checkout, Python setup, Java setup and artifact upload | The full commit pins in [security CI](.github/workflows/shm-api-guard.yml) and those projects' own license files |

MySQL Connector/J and other independently distributed components may have terms different from MIT. Preserve their license texts and notices when redistributing dependencies or assembled binaries; consult the exact resolved artifacts rather than treating the root license as a blanket replacement. No new binary distribution is made by this licensing change.

Vendor software, device firmware, vendor trademarks and operational measurement data are outside the grant. References to OS265 and other products identify interoperability and do not claim ownership or endorsement.
