use clap::Parser;
use std::path::PathBuf;

#[derive(Parser, Debug)]
#[command(author, version, about = "Flatten Hytale .ui macros and generate Java POMs")]
pub struct Args {
    #[arg(long)]
    pub resources_root: PathBuf,
    #[arg(long)]
    pub ui_root: PathBuf,
    #[arg(long)]
    pub ui_out: PathBuf,
    #[arg(long)]
    pub java_out: PathBuf,
    #[arg(long, default_value = "MBRound18.hytale.shared.interfaces.ui.generated")]
    pub java_package: String,
    #[arg(long)]
    pub java_root: Option<PathBuf>,
    #[arg(long)]
    pub lang_root: Option<PathBuf>,
    #[arg(long)]
    pub lang_class_file: Option<PathBuf>,
    #[arg(long)]
    pub lang_class_name: Option<String>,
    #[arg(long)]
    pub include_macro_only: bool,
    #[arg(long)]
    pub verbose: bool,
    #[arg(long)]
    pub warn_duplicate_properties: bool,
}
