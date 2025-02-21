# RepoDriller with SQLite Support

This is a modified version of [RepoDriller](https://github.com/mauricioaniche/repodriller), a tool for mining software repositories. This version introduces support for storing analysis results in an SQLite database, enabling more efficient data retrieval and management.

## Features

- Extracts and analyzes Git repositories, similar to the original RepoDriller.
- Stores analysis results in an SQLite database for easy querying.
- Maintains compatibility with the original RepoDriller API, allowing seamless integration into existing workflows.

## Use Case

This modified RepoDriller is used as a **dependency** in an IntelliJ IDEA plugin project that performs repository analysis. The SQLite storage feature ensures persistent and structured storage of analysis results, making it easier to manage large-scale repository insights.


## License

This project is licensed under the **Apache 2.0 License**, following the original RepoDriller licensing terms. See the `LICENSE` file for details.

## Credits

- Original RepoDriller: [Maurício Aniche](https://github.com/mauricioaniche/repodriller)
- Modifications and SQLite support: [Roman Naryshkin](https://github.com/tera201)

