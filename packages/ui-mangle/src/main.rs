use clap::Parser;
use ui_mangle::Args;

fn main() -> anyhow::Result<()> {
    let args = Args::parse();
    ui_mangle::run(args)
}
